// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import mu.KLogging
import net.postchain.base.*
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.ManagedNodeConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.managed.GTXManagedNodeDataSource
import net.postchain.managed.ManagedBlockchainConfigurationProvider
import net.postchain.managed.ManagedNodeDataSource
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ScalarHandler
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Postchain node instantiates infrastructure and blockchain
 * process manager.
 */
open class PostchainNode(val nodeConfigProvider: NodeConfigurationProvider) : Shutdownable {

    lateinit var processManager: BlockchainProcessManager
    protected lateinit var blockchainInfrastructure: BlockchainInfrastructure
    protected lateinit var blockchainConfigProvider: BlockchainConfigurationProvider
    protected lateinit var storage: Storage

    // New members
    lateinit var dataSource: ManagedNodeDataSource
    private val queryRunner = QueryRunner()
    private val longRes = ScalarHandler<Long>()
    var lastPeerListVersion: Long? = null
    var updaterThreadStarted = false
    protected val executor = Executors.newSingleThreadScheduledExecutor()
    ///

    companion object : KLogging()

    init {
        initialize()
    }

    private fun initialize() {
        val infrastructureFactory = BaseInfrastructureFactoryProvider().createInfrastructureFactory(nodeConfigProvider)
        blockchainInfrastructure = infrastructureFactory.makeBlockchainInfrastructure(nodeConfigProvider)
        blockchainConfigProvider = infrastructureFactory.makeBlockchainConfigurationProvider()
        storage = blockchainInfrastructure.makeStorage()
        processManager = infrastructureFactory.makeProcessManager(
                nodeConfigProvider, blockchainInfrastructure, blockchainConfigProvider)
    }

    fun startBlockchain(chainID: Long) {
        if (chainID == 0L) {
            dataSource = buildChain0ManagedDataSource()

            // Setting up managed data source to the nodeConfig
            (nodeConfigProvider as? ManagedNodeConfigurationProvider)
                    ?.setPeerInfoDataSource(dataSource)
                    ?: logger.warn { "Node config is not managed, no peer info updates possible" }

            // Setting up managed data source to the blockchainConfig
            (blockchainConfigProvider as? ManagedBlockchainConfigurationProvider)
                    ?.setDataSource(dataSource)
                    ?: logger.warn { "Blockchain config is not managed" }
        }

        processManager.startBlockchain(chainID)
        if (chainID == 0L) {
            startUpdaterThread()
        }
    }

    fun stopBlockchain(chainID: Long) {
        processManager.stopBlockchain(chainID)
    }

    override fun shutdown() {
        // FYI: Order is important
        processManager.shutdown()
        blockchainInfrastructure.shutdown()
    }


    ////////////////////////////////////////

    private fun buildChain0ManagedDataSource(): ManagedNodeDataSource {
        val chainId = 0L
        var blockQueries: BlockQueries? = null

        withWriteConnection(storage, chainId) { eContext ->
            val configuration = blockchainConfigProvider.getConfiguration(eContext, chainId)
                    ?: throw ProgrammerMistake("chain0 configuration not found")
            val blockchainRID = DatabaseAccess.of(eContext).getBlockchainRID(eContext)!! // TODO: [et]: Fix Kotlin NPE
            val context = BaseBlockchainContext(blockchainRID, NODE_ID_AUTO, chainId, null)
            val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(configuration, context)
            blockchainConfig.initializeDB(eContext)

            val storage = StorageBuilder.buildStorage(nodeConfigProvider.getConfiguration().appConfig, NODE_ID_NA)
            blockQueries = blockchainConfig.makeBlockQueries(storage)
            true
        }

        return GTXManagedNodeDataSource(blockQueries!!, nodeConfigProvider.getConfiguration())
    }

    private fun startUpdaterThread() {
        if (updaterThreadStarted) return
        updaterThreadStarted = true

        executor.scheduleWithFixedDelay(
                {
                    try {
                        runPeriodic()
                    } catch (e: Exception) {
                        logger.error("Unhandled exception in manager.runPeriodic", e)
                    }
                },
                10, 10, TimeUnit.SECONDS)
    }

    private fun runPeriodic() {
        val peerListVersion = dataSource.getPeerListVersion()
        val reloadBlockchains = (lastPeerListVersion != null) && (lastPeerListVersion != peerListVersion)
        lastPeerListVersion = peerListVersion
        logger.error { "Reloading of blockchains ${if (reloadBlockchains) "are" else "are not"} required" }

        // Reloading
        if (reloadBlockchains) {
            withWriteConnection(storage, 0) { ctx0 ->
                val blockchains = processManager.getBlockchains()

                val pubKey = nodeConfigProvider.getConfiguration().pubKey
                val peerInfos = nodeConfigProvider.getConfiguration().peerInfoMap
                logger.error {
                    "\n\n\n" + pubKey + " @ " + peerInfos.keys.toTypedArray().contentToString()
                }

                // Shutting down
                shutdown()

                // Starting BlockchainInfrastructure and ProcessManager
                initialize()

                // Starting blockchains
                //  * chain 0
                loadChain0Configuration(ctx0)
                startBlockchain(0L)
                //  * other chains
                blockchains
                        .filter { it != 0L }
                        .forEach { startBlockchain(it) }

                true
            }
        }
    }

    private fun loadChain0Configuration(ctx0: EContext) {
        val dbAccess = DatabaseAccess.of(ctx0)
        val brid = dbAccess.getBlockchainRID(ctx0)!!
        val height = dbAccess.getLastBlockHeight(ctx0)
        val nextConfigHeight = dataSource.findNextConfigurationHeight(brid, height)
        if (nextConfigHeight != null) {
            if (BaseConfigurationDataStore.findConfiguration(ctx0, nextConfigHeight) != nextConfigHeight) {
                val config = dataSource.getConfiguration(brid, nextConfigHeight)!!
                BaseConfigurationDataStore.addConfigurationData(ctx0, nextConfigHeight, config)
            }
        }
    }

    /*
// TODO: [et2]: Rename
// Restart chain0
maybeUpdateChain0(ctx, reloadBlockchains)
// Restart chain1, 2, ...
applyBlockchainList(ctx, dataSource.computeBlockchainList(ctx), reloadBlockchains)
*/

    @Deprecated("TODO: Used. Delete it")
    fun maybeUpdateChain0(ctx: EContext, reload: Boolean) {
        val dba = DatabaseAccess.of(ctx)
        val brid = dba.getBlockchainRID(ctx)!!
        val height = dba.getLastBlockHeight(ctx)
        val nextConfHeight = dataSource.findNextConfigurationHeight(brid, height)
        if (nextConfHeight != null) {
            if (BaseConfigurationDataStore.findConfiguration(ctx, nextConfHeight) != nextConfHeight) {
                BaseConfigurationDataStore.addConfigurationData(
                        ctx, nextConfHeight,
                        dataSource.getConfiguration(brid, nextConfHeight)!!
                )
            }
        }

        if (reload) {
            processManager.startBlockchainAsync(0)
        }
    }

    @Deprecated("TODO: Delete it")
    fun applyBlockchainList(ctx: EContext, list: List<ByteArray>, forceReload: Boolean) {
        val dba = DatabaseAccess.of(ctx)
        for (elt in list) {
            val ci = dba.getChainId(ctx, elt)
            if (ci == null) {
                addBlockchain(ctx, elt)
            } else if (ci != 0L) {
                val proc = processManager.retrieveBlockchain(ci)
                if (proc == null || forceReload)
                    processManager.startBlockchainAsync(ci)
            }
        }
    }

    @Deprecated("TODO: Delete it")
    fun addBlockchain(ctx: EContext, blockchainRID: ByteArray) {
        val dba = DatabaseAccess.of(ctx)
        // find the next unused chain_id starting from 100
        val new_chain_id = maxOf(
                queryRunner.query(ctx.conn, "SELECT MAX(chain_id) FROM blockchains", longRes) + 1,
                100)
        val newCtx = BaseEContext(ctx.conn, new_chain_id, ctx.nodeID, dba)
        dba.checkBlockchainRID(newCtx, blockchainRID)
        processManager.startBlockchainAsync(new_chain_id)
    }
}