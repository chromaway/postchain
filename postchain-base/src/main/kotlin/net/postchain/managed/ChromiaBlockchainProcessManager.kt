package net.postchain.managed

import net.postchain.base.BaseBlockWitness
import net.postchain.base.BaseBlockchainProcessManager
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.withReadConnection
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.RestartHandler
import net.postchain.devtools.KeyPairHelper
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXDataBuilder
import net.postchain.gtx.GTXTransactionFactory
import java.lang.Exception

class ChromiaBlockchainProcessManager(
        blockchainInfrastructure: BlockchainInfrastructure,
        nodeConfigProvider: NodeConfigurationProvider,
        blockchainConfigProvider: BlockchainConfigurationProvider
): ManagedBlockchainProcessManager(blockchainInfrastructure, nodeConfigProvider, blockchainConfigProvider) {

    fun anchorLastBlock(chainId: Long) {
        withReadConnection(storage, chainId) { eContext ->
            val dba = DatabaseAccess.of(eContext)
            val blockRID = dba.getLastBlockRid(eContext, eContext.chainID)
            val bcConfig = blockchainProcesses[chainId]!!.getEngine().getConfiguration()
            if (blockRID != null) {
                val blockHeader = dba.getBlockHeader(eContext, blockRID)
                val witnessData = dba.getWitnessData(eContext, blockRID)
                val witness = BaseBlockWitness.fromBytes(witnessData)
                val txb = GTXDataBuilder(bcConfig.blockchainRID, arrayOf(), SECP256K1CryptoSystem())
                txb.addOperation("anchor_block",
                        arrayOf(
                                GtvByteArray(blockHeader),
                                GtvArray(
                                        witness.getSignatures().map { GtvByteArray(it.subjectID) }.toTypedArray()
                                ),
                                GtvArray(
                                        witness.getSignatures().map { GtvByteArray(it.data) }.toTypedArray()
                                )
                                )
                )
                txb.finish()
                val chain0Engine = blockchainProcesses[0L]!!.getEngine()
                val tx = chain0Engine.getConfiguration().getTransactionFactory().decodeTransaction(
                        txb.serialize()
                )
                chain0Engine.getTransactionQueue().enqueue(tx)
            }
        }
    }

    override fun restartHandler(chainId: Long): RestartHandler {
        val baseHandler = super.restartHandler(chainId)
        if (chainId == 0L)
            return baseHandler
        else {
            return {
                try {
                    anchorLastBlock(chainId)
                } catch (e: Exception) {
                    logger.error("Error when anchoring ${e.toString()}")
                }
                baseHandler()
            }
        }
    }
}