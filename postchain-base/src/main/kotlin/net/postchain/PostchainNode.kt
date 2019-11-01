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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Postchain node instantiates infrastructure and blockchain process manager.
 */
open class PostchainNode(val nodeConfigProvider: NodeConfigurationProvider) : Shutdownable {

    lateinit var processManager: BlockchainProcessManager
    protected lateinit var blockchainInfrastructure: BlockchainInfrastructure
    protected lateinit var blockchainConfigProvider: BlockchainConfigurationProvider
    protected lateinit var storage: Storage
    protected var synchronizer = ReentrantLock()
    private lateinit var dataSource: ManagedNodeDataSource
    private var lastPeerListVersion: Long? = null
    private val executor = Executors.newSingleThreadExecutor()

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
                nodeConfigProvider, blockchainInfrastructure, blockchainConfigProvider, restartHandlerFactory())
        processManager.synchronizer = synchronizer
    }

    fun startBlockchain(chainID: Long): Boolean {
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

        return processManager.startBlockchain(chainID)
    }

    fun stopBlockchain(chainID: Long) {
        processManager.stopBlockchain(chainID)
    }

    override fun shutdown() {
        executor.shutdownNow()
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS)

        halt()
    }

    private fun halt() {
        // FYI: Order is important
        processManager.shutdown()
        blockchainInfrastructure.shutdown()
    }

    private fun restartHandlerFactory(): (chainId: Long) -> RestartHandler {

        fun restartHandlerChain0(): Boolean {
            return synchronizer.withLock {
                // Preloading blockchain configuration
                loadBlockchainConfiguration(0L)

                // Checking out for a peers set changes
                val peerListVersion = dataSource.getPeerListVersion()
                val doReload = (lastPeerListVersion != null) && (lastPeerListVersion != peerListVersion)
                lastPeerListVersion = peerListVersion

                if (doReload) {
                    logger.info { "Reloading of blockchains are required" }
                    reloadBlockchainsAsync()
                    true

                } else {
                    val toLaunch = retrieveBlockchainsToLaunch()
                    val launched = processManager.getBlockchains()

                    // Checking out for a chain0 configuration changes
                    val reloadBlockchainConfig = withReadConnection(storage, 0L) { eContext ->
                        blockchainConfigProvider.needsConfigurationChange(eContext, 0L)
                    }

                    // Launching blockchain 0
                    val reloadChan0 = 0L in toLaunch && (0L !in launched || reloadBlockchainConfig)
                    startStopBlockchainsAsync(toLaunch, launched, reloadChan0)
                    reloadChan0
                }
            }
        }

        fun restartHandler(chainId: Long): Boolean {
            return synchronizer.withLock {
                // Checking out for a chain configuration changes
                val reloadBlockchainConfig = withReadConnection(storage, chainId) { eContext ->
                    (blockchainConfigProvider.needsConfigurationChange(eContext, chainId))
                }

                if (reloadBlockchainConfig) {
                    reloadBlockchainConfigAsync(chainId)
                    true
                } else {
                    false
                }
            }
        }

        return { chainId ->
            {
                if (chainId == 0L) restartHandlerChain0() else restartHandler(chainId)
            }
        }
    }

    private fun reloadBlockchainsAsync() {
        executor.submit {
            val toLaunch = retrieveBlockchainsToLaunch()

            // Reloading
            // FYI: For testing only. It can be deleted later.
            logger.error {
                val pubKey = nodeConfigProvider.getConfiguration().pubKey
                val peerInfos = nodeConfigProvider.getConfiguration().peerInfoMap
                "reloadBlockchainsAsync: " +
                        "pubKey: $pubKey" +
                        ", peerInfos: ${peerInfos.keys.toTypedArray().contentToString()}, " +
                        ", chains to launch: ${toLaunch.contentDeepToString()}"
            }

            // TODO: [et]: COMMENT: Shutting down
            halt()

            // Starting BlockchainInfrastructure and ProcessManager
            initialize()

            // Starting blockchains: at first chain0, then the rest
            logger.info { "Launching blockchain 0" }
            startBlockchain(0L)

            toLaunch.filter { it != 0L }.forEach {
                logger.info { "Launching blockchain $it" }
                startBlockchain(it)
            }
        }
    }

    private fun startStopBlockchainsAsync(toLaunch: Array<Long>, launched: Set<Long>, reloadChain0: Boolean) {
        executor.submit {
            // Launching blockchain 0
            if (reloadChain0) {
                logger.info { "Reloading of blockchain 0 is required" }
                logger.info { "Launching blockchain 0" }
                processManager.startBlockchain(0L)
            }

            // Launching new blockchains except blockchain 0
            toLaunch.filter { it != 0L }
                    .filter { processManager.retrieveBlockchain(it) == null }
                    .forEach {
                        logger.info { "Launching blockchain $it" }
                        processManager.startBlockchain(it)
                    }

            // Stopping launched blockchains
            launched.filterNot(toLaunch::contains)
                    .filter { processManager.retrieveBlockchain(it) != null }
                    .forEach {
                        logger.info { "Stopping blockchain $it" }
                        processManager.stopBlockchain(it)
                    }
        }
    }

    private fun reloadBlockchainConfigAsync(chainId: Long) {
        executor.submit {
            processManager.startBlockchain(chainId)
        }
    }

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

    private fun loadBlockchainConfiguration(chainId: Long) {
        withWriteConnection(storage, chainId) { ctx ->
            val dbAccess = DatabaseAccess.of(ctx)
            val brid = dbAccess.getBlockchainRID(ctx)!!
            val height = dbAccess.getLastBlockHeight(ctx)
            val nextConfigHeight = dataSource.findNextConfigurationHeight(brid, height)
            if (nextConfigHeight != null) {
                logger.info { "Next config height fount in managed-mode module: $nextConfigHeight" }
                if (BaseConfigurationDataStore.findConfiguration(ctx, nextConfigHeight) != nextConfigHeight) {
                    logger.info {
                        "Configuration for the height $nextConfigHeight is not fount in ConfigurationDataStore " +
                                "and will be loaded into it from managed-mode module"
                    }
                    val config = dataSource.getConfiguration(brid, nextConfigHeight)!!
                    BaseConfigurationDataStore.addConfigurationData(ctx, nextConfigHeight, config)
                }
            }

            true
        }
    }

    private fun retrieveBlockchainsToLaunch(): Array<Long> {
        val blockchains = mutableListOf<Long>()

        withWriteConnection(storage, 0) { ctx0 ->
            val dba = DatabaseAccess.of(ctx0)
            dataSource.computeBlockchainList(ctx0)
                    .map { brid ->
                        val chainIid = dba.getChainId(ctx0, brid)
                        if (chainIid == null) {
                            val newChainId = maxOf(
                                    QueryRunner().query(ctx0.conn, "SELECT MAX(chain_iid) FROM blockchains", ScalarHandler<Long>()) + 1,
                                    100)
                            val newCtx = BaseEContext(ctx0.conn, newChainId, ctx0.nodeID, dba)
                            dba.checkBlockchainRID(newCtx, brid)
                            newChainId
                        } else {
                            chainIid
                        }
                    }
                    .forEach {
                        blockchains.add(it)
                    }

            true
        }

        return blockchains.toTypedArray()
    }
}
