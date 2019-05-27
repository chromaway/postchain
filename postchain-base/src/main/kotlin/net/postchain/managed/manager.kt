package net.postchain.managed

import net.postchain.base.*
import net.postchain.base.data.BaseStorage
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.blockchain.ManualBlockchainConfigurationProvider
import net.postchain.config.node.ManagedNodeConfigurationProvider
import net.postchain.config.node.NodeConfig
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.config.node.PeerInfoDataSource
import net.postchain.core.*
import net.postchain.ebft.BaseEBFTInfrastructureFactory
import net.postchain.gtv.GtvFactory.gtv
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ScalarHandler
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

class RealManagedBlockchainConfigurationProvider(val nodeConfigProvider: NodeConfigurationProvider)
    : BlockchainConfigurationProvider
{

    private lateinit var dataSource: ManagedNodeDataSource
    val systemProvider = ManualBlockchainConfigurationProvider(nodeConfigProvider)

    fun initDataSource(dataSource: ManagedNodeDataSource) {
        this.dataSource = dataSource
    }

    override fun getConfiguration(eContext: EContext, chainId: Long): ByteArray? {
        if (chainId == 0L) {
            return systemProvider.getConfiguration(eContext, chainId)
        } else {
            if (::dataSource.isInitialized) {
                val dba = DatabaseAccess.of(eContext)
                val newCtx = BaseEContext(eContext.conn,
                        eContext.chainID, eContext.nodeID, dba)
                val blockchainRID = dba.getBlockchainRID(newCtx)
                val height = dba.getLastBlockHeight(newCtx) + 1
                return dataSource.getConfiguration(blockchainRID!!, height)
            } else {
                throw IllegalStateException("Using managed blockchain configuration provider before it's properly initialized")
            }
        }
    }
}

interface ManagedNodeDataSource: PeerInfoDataSource {
    fun computeBlockchainList(ctx: EContext): List<ByteArray>
    fun getConfiguration(blockchainRID: ByteArray, height: Long): ByteArray?
}

class GTXManagedNodeDataSource(val q: BlockQueries, val nc: NodeConfig): ManagedNodeDataSource {
    override fun getPeerInfos(): Array<PeerInfo> {
        val res = q.query("nm_get_peer_infos", gtv(mapOf()))
        val a = res.get().asArray()
        return a.map {
            val pia = it.asArray()
            PeerInfo(
                    pia[0].asString(),
                    pia[1].asInteger().toInt(),
                    pia[2].asByteArray()
            )
        }.toTypedArray()
    }

    override fun computeBlockchainList(ctx: EContext): List<ByteArray> {
            val res = q.query("nm_compute_blockchain_list",
                    gtv("node_id" to gtv(nc.pubKeyByteArray))
            )
            val a = res.get().asArray()
            return a.map { it.asByteArray() }
    }

    override fun getConfiguration(blockchainRID: ByteArray, height: Long): ByteArray? {
        val res = q.query("nm_get_blockchain_configuration",
                gtv("blockchain_rid" to gtv(blockchainRID),
                        "height" to gtv(height))).get()
        if (res.isNull())
            return null
        else
            return res.asByteArray()
    }
}

class ManagedInfrastructureFactory: BaseEBFTInfrastructureFactory() {
    override fun makeProcessManager(
            nodeConfigProvider: NodeConfigurationProvider,
            blockchainInfrastructure: BlockchainInfrastructure
    ): BlockchainProcessManager {
        return ManagedBlockchainProcessManager(
                blockchainInfrastructure,
                nodeConfigProvider
        )
    }
}

class Manager(val procMan: BlockchainProcessManager,
              val nodeConfigProvider: NodeConfigurationProvider) {

    private val queryRunner = QueryRunner()
    private val longRes = ScalarHandler<Long>()
    lateinit var dataSource: ManagedNodeDataSource

    fun init(proc0: BlockchainProcess): ManagedNodeDataSource {
        dataSource = GTXManagedNodeDataSource(proc0.getEngine().getBlockQueries(),
                nodeConfigProvider.getConfiguration()
        )
        return dataSource
    }

    fun applyBlockchainList(ctx: EContext, list: List<ByteArray>) {
        val dba = DatabaseAccess.of(ctx)
        for (elt in list) {
            val ci = dba.getChainId(ctx, elt)
            if (ci == null) {
                addBlockchain(ctx, elt)
            } else {
                val proc = procMan.retrieveBlockchain(ci)
                if (proc == null)
                    procMan.startBlockchainAsync(ci)
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
        procMan.startBlockchainAsync(new_chain_id)
    }

    fun runPeriodic(ctx: EContext) {
        applyBlockchainList(ctx, dataSource.computeBlockchainList(ctx))
    }
}

class ManagedBlockchainProcessManager(
    blockchainInfrastructure: BlockchainInfrastructure,
    nodeConfigProvider: NodeConfigurationProvider
): BaseBlockchainProcessManager(
        blockchainInfrastructure,
        nodeConfigProvider,
        RealManagedBlockchainConfigurationProvider(nodeConfigProvider)
)
{
    val manager = Manager(this, nodeConfigProvider)

    fun startUpdaterThread() {
        executor.scheduleWithFixedDelay(
                {
                    try {
                        withWriteConnection(storage, 0) {
                            manager.runPeriodic(it)
                            true
                        }
                    } catch (e: Exception) {
                        logger.error("Unhandled exception in manager.runPeriodic", e.stackTrace)
                    }
                },
                10, 10, TimeUnit.SECONDS
        )
    }

    override fun startBlockchain(chainId: Long) {
        super.startBlockchain(chainId)
        if (chainId == 0L) {
            val dataSource = manager.init(retrieveBlockchain(0L)!!)
            (blockchainConfigProvider as RealManagedBlockchainConfigurationProvider).initDataSource(dataSource)
            if (nodeConfigProvider is ManagedNodeConfigurationProvider) {
                nodeConfigProvider.setPeerInfoDataSource(dataSource)
            } else {
                logger.warn { "Node config is not managed, no peer info updates possible" }
            }
            startUpdaterThread()
        }
    }
}