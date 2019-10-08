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
@Suppress("UNUSED_VARIABLE")
open class PostchainNode(val nodeConfigProvider: NodeConfigurationProvider) : Shutdownable {

    lateinit var processManager: BlockchainProcessManager
    protected lateinit var blockchainInfrastructure: BlockchainInfrastructure
    protected lateinit var blockchainConfigProvider: BlockchainConfigurationProvider
    protected lateinit var storage: Storage
    protected var synchronizer = ReentrantLock()

    // New members
    private lateinit var dataSource: ManagedNodeDataSource
    private val queryRunner = QueryRunner()
    private val longRes = ScalarHandler<Long>()
    private var lastPeerListVersion: Long? = null
    private var updaterThreadStarted = false
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val executor2 = Executors.newSingleThreadExecutor()
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
                nodeConfigProvider, blockchainInfrastructure, blockchainConfigProvider, restartHandlerFactory())
        processManager.synchronizer = synchronizer
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

        /*if (chainID == 0L) {
            startUpdaterThread()
        }*/
    }

    fun stopBlockchain(chainID: Long) {
        processManager.stopBlockchain(chainID)
    }

    override fun shutdown() {
        executor.shutdownNow()
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS)

        executor2.shutdownNow()
        executor2.awaitTermination(1000, TimeUnit.MILLISECONDS)

        halt()
    }

    private fun halt() {
        // FYI: Order is important
        processManager.shutdown()
        blockchainInfrastructure.shutdown()
    }

    private fun restartHandlerFactory(): (chainId: Long) -> RestartHandler {
        return { chainId ->
            {
                // Preloading blockchain configuration
                loadBlockchainConfiguration(chainId)

                // Checking out for a peers set changes
                val peerListVersion = dataSource.getPeerListVersion()
                val reloadBlockchains = (lastPeerListVersion != null) && (lastPeerListVersion != peerListVersion)
                lastPeerListVersion = peerListVersion
                logger.error { "Reloading of blockchains ${if (reloadBlockchains) "are" else "are not"} required" }

                if (reloadBlockchains) {
                    reloadBlockchainsAsync()
                    true

                } else {
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
        }
    }

    private fun reloadBlockchainsAsync() {
        executor2.submit {
            val toLaunch = retrieveBlockchainsToLaunch()

            // Reloading
            // FYI: For testing only. It can be deleted later.
            logger.error {
                val pubKey = nodeConfigProvider.getConfiguration().pubKey
                val peerInfos = nodeConfigProvider.getConfiguration().peerInfoMap
                "runPeriodic: " + pubKey + " @ " + peerInfos.keys.toTypedArray().contentToString()
            }

            // TODO: [et]: COMMENT: Shutting down
            halt()

            // Starting BlockchainInfrastructure and ProcessManager
            initialize()

            // Starting blockchains: at first chain0, then the rest
//            loadBlockchainConfiguration(0L)
            startBlockchain(0L)
            toLaunch.filter { it != 0L }.forEach { startBlockchain(it) }
        }
    }

    private fun reloadBlockchainConfigAsync(chainId: Long) {
        executor2.submit {
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

    private var i = 0
    private fun startUpdaterThread() {
        if (updaterThreadStarted) return
        updaterThreadStarted = true

        executor.scheduleWithFixedDelay(
                {
                    try {
                        runPeriodic(++i)
                    } catch (e: Exception) {
                        logger.error("Unhandled exception in PostchainNode.runPeriodic()", e)
                    }
                },
                10, 10, TimeUnit.SECONDS)
    }

    private fun runPeriodic(i: Int) {
        synchronizer.withLock {
            runPeriodicImpl(i)
        }
    }

    private fun runPeriodicImpl(i: Int) {
//        logger.error { "@@@ [${nodeConfigProvider.getConfiguration().pubKey.takeLast(4)}] BEGIN $i" }


        val peerListVersion = dataSource.getPeerListVersion()
        val reloadBlockchains = (lastPeerListVersion != null) && (lastPeerListVersion != peerListVersion)
        lastPeerListVersion = peerListVersion
        logger.error { "Reloading of blockchains ${if (reloadBlockchains) "are" else "are not"} required" }

        // Making some preparation
        // * Loading chain0 configuration
//        loadChain0Configuration()
        // * Retrieving blockchains list to launch
        val toLaunch = retrieveBlockchainsToLaunch()

        // Reloading
        if (reloadBlockchains) {
            // FYI: For testing only. It can be deleted later.
            logger.error {
                val pubKey = nodeConfigProvider.getConfiguration().pubKey
                val peerInfos = nodeConfigProvider.getConfiguration().peerInfoMap
                "runPeriodic: " + pubKey + " @ " + peerInfos.keys.toTypedArray().contentToString()
            }

            // TODO: [et]: COMMENT: Shutting down
            halt()

            // Starting BlockchainInfrastructure and ProcessManager
            initialize()

            // Starting blockchains: at first chain0, then the rest
            loadBlockchainConfiguration(0L)
            startBlockchain(0L)
            toLaunch.filter { it != 0L }.forEach { startBlockchain(it) }

        } else {
//            loadChain0Configuration()

            /*
            val launched = processManager.getBlockchains()

            // Launching new blockchains
            toLaunch.filter { processManager.retrieveBlockchain(it) == null }
//                    .forEach(processManager::startBlockchain)
                    .forEach {
                        logger.error { "\n\n\n### $it" }
                        processManager.startBlockchain(it)
                    }

            // Stopping launched blockchains
            launched.filterNot(toLaunch::contains)
                    .filter { processManager.retrieveBlockchain(it) != null }
//                    .forEach(processManager::stopBlockchain)
                    .forEach {
                        logger.error { "\n\n\n###### $it" }
                        processManager.stopBlockchain(it)
                    }

             */
        }

//        logger.error { "@@@ [${nodeConfigProvider.getConfiguration().pubKey.takeLast(4)}] END $i" }
    }

    private fun loadBlockchainConfiguration(chainId: Long) {
        withWriteConnection(storage, chainId) { ctx ->
            val dbAccess = DatabaseAccess.of(ctx)
            val brid = dbAccess.getBlockchainRID(ctx)!!
            val height = dbAccess.getLastBlockHeight(ctx)
            val nextConfigHeight = dataSource.findNextConfigurationHeight(brid, height)
            if (nextConfigHeight != null) {
                if (BaseConfigurationDataStore.findConfiguration(ctx, nextConfigHeight) != nextConfigHeight) {
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
                                    queryRunner.query(ctx0.conn, "SELECT MAX(chain_id) FROM blockchains", longRes) + 1,
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
