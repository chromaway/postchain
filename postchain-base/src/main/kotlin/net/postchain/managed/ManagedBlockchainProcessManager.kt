package net.postchain.managed

import net.postchain.StorageBuilder
import net.postchain.base.*
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.ManagedNodeConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ScalarHandler
import java.util.concurrent.TimeUnit

class ManagedBlockchainProcessManager(
        blockchainInfrastructure: BlockchainInfrastructure,
        nodeConfigProvider: NodeConfigurationProvider,
        blockchainConfigProvider: BlockchainConfigurationProvider

) : BaseBlockchainProcessManager(
        blockchainInfrastructure,
        nodeConfigProvider,
        blockchainConfigProvider
) {

    //    val manager = Manager(this, nodeConfigProvider)
    var updaterThreadStarted = false

    override fun startBlockchain(chainId: Long) {
        if (chainId == 0L) {
            val chain0BlockQueries = makeChain0BlockQueries()
            val dataSource = useChain0BlockQueries(chain0BlockQueries)
            (blockchainConfigProvider as ManagedBlockchainConfigurationProvider).setDataSource(dataSource)
            if (nodeConfigProvider is ManagedNodeConfigurationProvider) {
                nodeConfigProvider.setPeerInfoDataSource(dataSource)
            } else {
                logger.warn { "Node config is not managed, no peer info updates possible" }
            }
        }

        super.startBlockchain(chainId)
        if (chainId == 0L) {
            startUpdaterThread()
        }
    }

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
            val storage = StorageBuilder.buildStorage(nodeConfigProvider.getConfiguration(), NODE_ID_NA)
            blockQueries = blockchainConfig.makeBlockQueries(storage)
            true
        }

        return blockQueries!!
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
                        logger.error("Unhandled exception in manager.runPeriodic", e)
                    }
                },
                10, 10, TimeUnit.SECONDS
        )
    }

    /**
     * Manager class
     */

    private val queryRunner = QueryRunner()
    private val longRes = ScalarHandler<Long>()
    lateinit var dataSource: ManagedNodeDataSource
    var lastPeerListVersion: Long? = null

    fun useChain0BlockQueries(bq: BlockQueries): ManagedNodeDataSource {
        dataSource = GTXManagedNodeDataSource(bq, nodeConfigProvider.getConfiguration())
        return dataSource
    }

    fun applyBlockchainList(ctx: EContext, list: List<ByteArray>, forceReload: Boolean) {
        val dba = DatabaseAccess.of(ctx)
        for (elt in list) {
            val ci = dba.getChainId(ctx, elt)
            if (ci == null) {
                addBlockchain(ctx, elt)
            } else if (ci != 0L) {
                val proc = retrieveBlockchain(ci)
                if (proc == null || forceReload)
                    startBlockchainAsync(ci)
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
        startBlockchainAsync(new_chain_id)
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
        if (reload)
            startBlockchainAsync(0)
    }

    fun runPeriodic(ctx: EContext) {
        val peerListVersion = dataSource.getPeerListVersion()
        val reloadBlockchains = (lastPeerListVersion != null) && (lastPeerListVersion != peerListVersion)
        lastPeerListVersion = peerListVersion

        maybeUpdateChain0(ctx, reloadBlockchains)
        applyBlockchainList(ctx, dataSource.computeBlockchainList(ctx), reloadBlockchains)
    }
}
