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
import net.postchain.debug.NodeDiagnosticContext
import net.postchain.devtools.KeyPairHelper
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXDataBuilder
import net.postchain.gtx.GTXTransactionFactory
import java.lang.Exception

class Chromia0BlockchainProcessManager(
        blockchainInfrastructure: BlockchainInfrastructure,
        nodeConfigProvider: NodeConfigurationProvider,
        blockchainConfigProvider: BlockchainConfigurationProvider,
        nodeDiagnosticContext: NodeDiagnosticContext
): ManagedBlockchainProcessManager(blockchainInfrastructure, nodeConfigProvider,
        blockchainConfigProvider, nodeDiagnosticContext) {

    fun anchorLastBlock(chainId: Long) {
        withReadConnection(storage, chainId) { eContext ->
            val dba = DatabaseAccess.of(eContext)
            val blockRID = dba.getLastBlockRid(eContext, chainId)
            val bcConfig = blockchainProcesses[chainId]!!.getEngine().getConfiguration()
            val chain0Engine = blockchainProcesses[0L]!!.getEngine()
            if (blockRID != null) {
                val blockHeader = dba.getBlockHeader(eContext, blockRID)
                val witnessData = dba.getWitnessData(eContext, blockRID)
                val witness = BaseBlockWitness.fromBytes(witnessData)
                val txb = GTXDataBuilder(chain0Engine.getConfiguration().blockchainRID,
                        arrayOf(), SECP256K1CryptoSystem())
                txb.addOperation("anchor_block",
                        arrayOf(
                                GtvDecoder.decodeGtv(blockHeader),
                                GtvArray(
                                        witness.getSignatures().map { GtvByteArray(it.subjectID) }.toTypedArray()
                                ),
                                GtvArray(
                                        witness.getSignatures().map { GtvByteArray(it.data) }.toTypedArray()
                                )
                                )
                )
                txb.finish()
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
                    e.printStackTrace()
                }
                baseHandler()
            }
        }
    }
}