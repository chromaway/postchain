package net.postchain.managed

import net.postchain.base.BaseBlockchainProcessManager
import net.postchain.base.BaseEContext
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.BlockchainProcessManager
import net.postchain.core.EContext
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvFactory.gtv
import nl.komponents.kovenant.Promise
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ScalarHandler
import kotlin.concurrent.thread

interface ManagedNodeDataSource {
    fun computeBlockchainList(ctx: EContext): List<ByteArray>
}

class GTXManagedNodeDataSource(val q: BlockQueries): ManagedNodeDataSource {
    override fun computeBlockchainList(ctx: EContext): List<ByteArray> {
            val res = q.query("m_compute_blockchain_list", GtvArray(arrayOf()))
            val a = res.get().asArray()
            return a.map { it.asByteArray() }
    }
}

class ManagedNodeHLOperator(val ll : ManagedNodeLLOperator) {
    fun applyBlockchainList(ctx: EContext, list: List<ByteArray>) {
        val dba = DatabaseAccess.of(ctx)
        for (elt in list) {
            val ci = dba.getChainId(ctx, elt)
            if (ci == null) {
                ll.addBlockchain(ctx, elt)
            }
        }
    }
}

class  ManagedNodeLLOperator(val procMan: BlockchainProcessManager) {
    private val queryRunner = QueryRunner()
    private val longRes = ScalarHandler<Long>()

    fun addBlockchain(ctx: EContext, blockchainRID: ByteArray) {
        val dba = DatabaseAccess.of(ctx)
        // find the next unused chain_id starting from 100
        val new_chain_id = maxOf(queryRunner.query("SELECT MAX(chain_id) FROM blockchains", longRes), 100)
        val newCtx = BaseEContext(ctx.conn, ctx.chainID, ctx.nodeID, dba)
        dba.checkBlockchainRID(newCtx, blockchainRID)
        procMan.startBlockchain(new_chain_id)
    }
}

class ManagedBlockchainProcessManager(
    private val blockchainInfrastructure: BlockchainInfrastructure,
    private val nodeConfigProvider: NodeConfigurationProvider,
    private val blockchainConfigProvider: BlockchainConfigurationProvider
): BaseBlockchainProcessManager(blockchainInfrastructure, nodeConfigProvider, blockchainConfigProvider)
{
    val updaterThread = thread {



        while (true) {

        }
    }

    override fun shutdown() {
        updaterThread.interrupt()
        super.shutdown()
    }


}