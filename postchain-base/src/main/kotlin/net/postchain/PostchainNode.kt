// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.*
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.ManagedNodeConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.core.Infrastructures.BaseEbft
import net.postchain.core.Infrastructures.BaseTest
import net.postchain.ebft.BaseEBFTInfrastructureFactory
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

    val processManager: BlockchainProcessManager
    protected val blockchainInfrastructure: BlockchainInfrastructure
    protected val blockchainConfigProvider: BlockchainConfigurationProvider
    protected val storage: Storage

    // New members
    lateinit var dataSource: ManagedNodeDataSource
    private val queryRunner = QueryRunner()
    private val longRes = ScalarHandler<Long>()
    var lastPeerListVersion: Long? = null
    var updaterThreadStarted = false
    protected val executor = Executors.newSingleThreadScheduledExecutor()
    ///

    init {
        val infrastructureFactory = buildInfrastructureFactory(nodeConfigProvider)
        blockchainInfrastructure = infrastructureFactory.makeBlockchainInfrastructure(nodeConfigProvider)
        blockchainConfigProvider = infrastructureFactory.makeBlockchainConfigurationProvider()
        storage = blockchainInfrastructure.makeStorage()
        processManager = infrastructureFactory.makeProcessManager(
                nodeConfigProvider, blockchainInfrastructure, blockchainConfigProvider)
    }

    fun startBlockchain(chainID: Long) {
        if (chainID == 0L) {
            val dataSource = useChain0BlockQueries(makeChain0BlockQueries())

            // Setting up managed data source to the nodeConfig
            if (nodeConfigProvider is ManagedNodeConfigurationProvider) {
                nodeConfigProvider.setPeerInfoDataSource(dataSource)
            } else {
                BaseBlockchainProcessManager.logger.warn { "Node config is not managed, no peer info updates possible" }
            }

            // Setting up managed data source to the blockchainConfig
            if (blockchainConfigProvider is ManagedBlockchainConfigurationProvider) {
                blockchainConfigProvider.setDataSource(dataSource)
            } else {
                BaseBlockchainProcessManager.logger.warn { "Blockchain config is not managed" }
            }
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
        processManager.shutdown()
    }

    private fun buildInfrastructureFactory(nodeConfigProvider: NodeConfigurationProvider): InfrastructureFactory {
        val infrastructureIdentifier = nodeConfigProvider.getConfiguration().infrastructure
        val factoryClass = when (infrastructureIdentifier) {
            BaseEbft.secondName.toLowerCase() -> BaseEBFTInfrastructureFactory::class.java
            BaseTest.secondName.toLowerCase() -> BaseTestInfrastructureFactory::class.java
            else -> Class.forName(infrastructureIdentifier)
        }
        return factoryClass.newInstance() as InfrastructureFactory
    }


    ////////////////////////////////////////

    private fun makeChain0BlockQueries(): BlockQueries {
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

        return blockQueries!!
    }

    private fun useChain0BlockQueries(blockQueries: BlockQueries): ManagedNodeDataSource {
        dataSource = GTXManagedNodeDataSource(blockQueries, nodeConfigProvider.getConfiguration())
        return dataSource
    }

    private fun startUpdaterThread() {
        if (updaterThreadStarted) return
        updaterThreadStarted = true

        executor.scheduleWithFixedDelay(
                {
                    try {
                        withWriteConnection(storage, 0) {
                            runPeriodic(it)
                            true
                        }
                    } catch (e: Exception) {
                        BaseBlockchainProcessManager.logger.error("Unhandled exception in manager.runPeriodic", e)
                    }
                },
                10, 10, TimeUnit.SECONDS)
    }

    fun runPeriodic(ctx: EContext) {
        val peerListVersion = dataSource.getPeerListVersion()
        val reloadBlockchains = (lastPeerListVersion != null) && (lastPeerListVersion != peerListVersion)
        lastPeerListVersion = peerListVersion

        BaseBlockchainProcessManager.logger.error { "Reloading of blockchains ${if (reloadBlockchains) "are" else "are not"} required" }

        // TODO: [et2]: Rename
        // Restart chain0
        maybeUpdateChain0(ctx, reloadBlockchains)
        // Restart chain1, 2, ...
        applyBlockchainList(ctx, dataSource.computeBlockchainList(ctx), reloadBlockchains)
    }

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