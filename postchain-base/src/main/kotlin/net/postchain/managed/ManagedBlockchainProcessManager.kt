// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.managed

import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.base.*
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.ManagedNodeConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.debug.NodeDiagnosticContext

/**
 * Extends on the [BaseBlockchainProcessManager] with managed mode. "Managed" means that the nodes automatically
 * share information about configuration changes, where "manual" means manual configuration on each node.
 *
 * Background
 * ----------
 * When the configuration of a blockchain has changed, there is a block height specified from when the new
 * BC config should be used, and before a block of this height is built the chain should be restarted so the
 * new config settings can be applied (this is the way "manual" mode works too, so nothing new about that).
 *
 * New
 * ----
 * What is unique with "managed mode" is that a blockchain is used for storing the configurations of the other chains.
 * This "config blockchain" is called "chain zero" (because it has chainIid == 0). By updating the configuration in
 * chain zero, the changes will spread to all nodes the normal way (via EBFT). We still have to restart a chain
 * every time somebody updates its config.
 *
 * A great deal of work in this class has to do with the [RestartHandler], which is usually called after a block
 * has been build to see if we need to upgrade anything about the chain's configuration.
 *
 * Most of the logic in this class is about the case when we need to check chain zero itself, and the most serious
 * case is when the peer list of the chain zero has changed (in this case restarting chains will not be enough).
 *
 * Doc: see the /doc/postchain_ManagedModeFlow.graphml (created with yEd)
 *
 */
open class ManagedBlockchainProcessManager(
        blockchainInfrastructure: BlockchainInfrastructure,
        nodeConfigProvider: NodeConfigurationProvider,
        blockchainConfigProvider: BlockchainConfigurationProvider,
        nodeDiagnosticContext: NodeDiagnosticContext
) : BaseBlockchainProcessManager(
        blockchainInfrastructure,
        nodeConfigProvider,
        blockchainConfigProvider,
        nodeDiagnosticContext
) {

    private lateinit var dataSource: ManagedNodeDataSource
    private var lastPeerListVersion: Long? = null

    companion object : KLogging()

    /**
     * Check if this is the "chain zero" and if so we need to set the dataSource in a few objects before we go on.
     */
    override fun startBlockchain(chainId: Long): BlockchainRid? {
        try {
            if (chainId == 0L) {
                dataSource = buildChain0ManagedDataSource()

                // TODO: [POS-97]: Put this to DiagnosticContext
//                logger.debug { "${nodeConfigProvider.javaClass}" }

                // Setting up managed data source to the nodeConfig
                (nodeConfigProvider as? ManagedNodeConfigurationProvider)
                        ?.setPeerInfoDataSource(dataSource)
                        ?: logger.warn { "Node config is not managed, no peer info updates possible" }

                // TODO: [POS-97]: Put this to DiagnosticContext
//                logger.debug { "${blockchainConfigProvider.javaClass}" }

                // Setting up managed data source to the blockchainConfig
                (blockchainConfigProvider as? ManagedBlockchainConfigurationProvider)
                        ?.setDataSource(dataSource)
                        ?: logger.warn { "Blockchain config is not managed" }
            }

        } catch (e: Exception) {
            // TODO: [POS-90]: Improve error handling here
            logger.error { e.message }
        }

        return super.startBlockchain(chainId)
    }

    /**
     * @return a [RestartHandler] which is a lambda (This lambda will be called by the Engine after each block
     *          has been committed.)
     */
    override fun restartHandler(chainId: Long): RestartHandler {

        /**
         * If the chain we are checking is the chain zero itself, we must verify if the list of peers have changed.
         * A: If we have new peers we will need to restart the node (or update the peer connections somehow).
         * B: If not, we just check with chain zero what chains we need and run those.
         */
        fun restartHandlerChain0(): Boolean {

            // Preloading blockchain configuration
            loadBlockchainConfiguration(0L)

            // Checking out for a peers set changes
            val peerListVersion = dataSource.getPeerListVersion()
            val doReload = (lastPeerListVersion != null) && (lastPeerListVersion != peerListVersion)
            lastPeerListVersion = peerListVersion

            return if (doReload) {
                logger.info { "Reloading of blockchains are required" }
                reloadBlockchainsAsync()
                true

            } else {
                val toLaunch = retrieveBlockchainsToLaunch()
                val launched = blockchainProcesses.keys

                // Checking out for a chain0 configuration changes
                val reloadBlockchainConfig = withReadConnection(storage, 0L) { eContext ->
                    blockchainConfigProvider.needsConfigurationChange(eContext, 0L)
                }

                startStopBlockchainsAsync(toLaunch, launched, reloadBlockchainConfig)
                reloadBlockchainConfig
            }
        }

        /**
         * If it's not the chain zero we are looking at, all we need to do is:
         * a) see if configuration has changed and
         * b) restart the chain if this is the case.
         *
         * @param chainId is the chain we should check (cannot be chain zero).
         */
        fun restartHandlerChainN(): Boolean {
            // Checking out for a chain configuration changes
            val reloadBlockchainConfig = withReadConnection(storage, chainId) { eContext ->
                (blockchainConfigProvider.needsConfigurationChange(eContext, chainId))
            }

            return if (reloadBlockchainConfig) {
                reloadBlockchainConfigAsync(chainId)
                true
            } else {
                false
            }
        }

        fun wrappedRestartHandler(): Boolean {
            return try {
                synchronized(synchronizer) {
                    if (chainId == 0L) restartHandlerChain0() else restartHandlerChainN()
                }
            } catch (e: Exception) {
                logger.error("Exception in restard handler: ${e.toString()}")
                e.printStackTrace()
                reloadBlockchainConfigAsync(chainId)
                true // let's hope restarting a blockchain fixes the problem
            }
        }

        return ::wrappedRestartHandler
    }

    private fun buildChain0ManagedDataSource(): ManagedNodeDataSource {
        val chain0 = 0L
        val storage = StorageBuilder.buildStorage(
                nodeConfigProvider.getConfiguration().appConfig, NODE_ID_NA)

        val blockQueries = withReadWriteConnection(storage, chain0) { ctx0 ->
            val configuration = blockchainConfigProvider.getConfiguration(ctx0, chain0)
                    ?: throw ProgrammerMistake("chain0 configuration not found")

            val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(
                    configuration, ctx0, NODE_ID_AUTO, chain0)

            blockchainConfig.makeBlockQueries(storage)
        }

        return GTXManagedNodeDataSource(blockQueries, nodeConfigProvider.getConfiguration())
    }

    /**
     * Restart all chains. Begin with chain zero.
     */
    private fun reloadBlockchainsAsync() {
        executor.submit {
            val toLaunch = retrieveBlockchainsToLaunch()

            // Reloading
            // FYI: For testing only. It can be deleted later.

            logger.info {
                val pubKey = nodeConfigProvider.getConfiguration().pubKey
                val peerInfos = nodeConfigProvider.getConfiguration().peerInfoMap
                "reloadBlockchainsAsync: " +
                        "pubKey: $pubKey" +
                        ", peerInfos: ${peerInfos.keys.toTypedArray().contentToString()}" +
                        ", chains to launch: ${toLaunch.contentDeepToString()}"
            }


            // Starting blockchains: at first chain0, then the rest
            logger.info { "Launching blockchain 0" }
            startBlockchain(0L)

            toLaunch.filter { it != 0L }.forEach {
                logger.info { "Launching blockchain $it" }
                startBlockchain(it)
            }
        }
    }

    /**
     * Only the chains in the [toLaunch] list should run. Any old chains not in this list must be stopped.
     * Note: any chains not in the new config for this node should actually also be deleted, but not impl yet.
     *
     * @param toLaunch the chains to run
     * @param launched is the old chains. Maybe stop some of them.
     * @param reloadChain0 is true if the chain zero must be restarted.
     */
    private fun startStopBlockchainsAsync(toLaunch: Array<Long>, launched: Set<Long>, reloadChain0: Boolean) {
        executor.submit {
            // Launching blockchain 0
            if (reloadChain0) {
                logger.info { "Reloading of blockchain 0 is required" }
                logger.info { "Launching blockchain 0" }
                startBlockchain(0L)
            }

            if (logger.isDebugEnabled) {
                val toL = toLaunch.map { it.toString() }.reduce { s1, s2 -> "$s1 , $s2" }
                val l = launched.map { it.toString() }.reduce { s1, s2 -> "$s1 , $s2" }
                logger.debug("Chains to launch: $toL. Chains already launched: $l")
            }

            // Launching new blockchains except blockchain 0
            toLaunch.filter { it != 0L }
                    .filter { retrieveBlockchain(it) == null }
                    .forEach {
                        logger.info { "Launching blockchain $it" }
                        startBlockchain(it)
                    }

            // Stopping launched blockchains
            launched.filterNot(toLaunch::contains)
                    .filter { retrieveBlockchain(it) != null }
                    .forEach {
                        logger.info { "Stopping blockchain $it" }
                        stopBlockchain(it)
                    }
        }
    }

    private fun reloadBlockchainConfigAsync(chainId: Long) {
        executor.submit {
            startBlockchain(chainId)
        }
    }

    /**
     * Makes sure the next configuration is stored in DB.
     *
     * @param chainId is the chain we are interested in.
     */
    private fun loadBlockchainConfiguration(chainId: Long) {
        withWriteConnection(storage, chainId) { ctx ->
            val db = DatabaseAccess.of(ctx)
            val brid = db.getBlockchainRid(ctx)!! // We can only load chains this way if we know their BC RID.
            val height = db.getLastBlockHeight(ctx)
            val nextConfigHeight = dataSource.findNextConfigurationHeight(brid.data, height)
            if (nextConfigHeight != null) {
                logger.info { "Next config height found in managed-mode module: $nextConfigHeight" }
                if (BaseConfigurationDataStore.findConfigurationHeightForBlock(ctx, nextConfigHeight) != nextConfigHeight) {
                    logger.info {
                        "Configuration for the height $nextConfigHeight is not found in ConfigurationDataStore " +
                                "and will be loaded into it from managed-mode module"
                    }
                    val config = dataSource.getConfiguration(brid.data, nextConfigHeight)!!
                    BaseConfigurationDataStore.addConfigurationData(ctx, nextConfigHeight, config)
                }
            }

            true
        }
    }

    /**
     * Will call chain zero to ask what chains to run.
     *
     * Note: We use [computeBlockchainList()] which is the API method "nm_compute_blockchain_list" of this node's own
     * API for chain zero.
     *
     * @return all chainIids chain zero thinks we should run.
     */
    private fun retrieveBlockchainsToLaunch(): Array<Long> {
        // chain-zero is always in the list
        val blockchains = mutableListOf(0L)

        withWriteConnection(storage, 0) { ctx0 ->
            val db = DatabaseAccess.of(ctx0)
            dataSource.computeBlockchainList()
                    .map { brid ->
                        val blockchainRid = BlockchainRid(brid)
                        val chainId = db.getChainId(ctx0, blockchainRid)
                        logger.debug("Computed bc list: chainIid: $chainId,  BC RID: ${blockchainRid.toShortHex()}  ")
                        if (chainId == null) {
                            val newChainId = db.getMaxChainId(ctx0)
                                    ?.let { maxOf(it + 1, 100) }
                                    ?: 100
                            val newCtx = storage.newWritableContext(newChainId)
                            db.initializeBlockchain(newCtx, blockchainRid)
                            newChainId
                        } else {
                            chainId
                        }
                    }
                    .filter { it != 0L }
                    .forEach {
                        blockchains.add(it)
                    }

            true
        }

        return blockchains.toTypedArray()
    }

    // TODO: [POS-90]: Redesign this
    private fun inManagedMode(): Boolean {
        // TODO: We are using isInitialized as a measure of being in managed mode. Doesn't seem right?
//        logger.warn("We are using isInitialized as a measure of being in managed mode. Doesn't seem right? ")
        return ::dataSource.isInitialized
    }
}