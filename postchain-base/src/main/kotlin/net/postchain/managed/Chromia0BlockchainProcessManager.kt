// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.managed

import net.postchain.base.BaseBlockWitness
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.withReadConnection
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.BlockchainInfrastructure
import net.postchain.core.ByteArrayKey
import net.postchain.core.RestartHandler
import net.postchain.debug.NodeDiagnosticContext
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvDecoder
import net.postchain.gtx.GTXDataBuilder

class Chromia0BlockchainProcessManager(
        blockchainInfrastructure: BlockchainInfrastructure,
        nodeConfigProvider: NodeConfigurationProvider,
        blockchainConfigProvider: BlockchainConfigurationProvider,
        nodeDiagnosticContext: NodeDiagnosticContext
) : ManagedBlockchainProcessManager(blockchainInfrastructure, nodeConfigProvider,
        blockchainConfigProvider, nodeDiagnosticContext) {

    private fun anchorLastBlock(chainId: Long) {
        withReadConnection(storage, chainId) { eContext ->
            val dba = DatabaseAccess.of(eContext)
            val blockRID = dba.getLastBlockRid(eContext, chainId)
            val chain0Engine = blockchainProcesses[0L]!!.getEngine()
            if (blockRID != null) {
                val blockHeader = dba.getBlockHeader(eContext, blockRID)
                val witnessData = dba.getWitnessData(eContext, blockRID)
                val witness = BaseBlockWitness.fromBytes(witnessData)
                val txb = GTXDataBuilder(chain0Engine.getConfiguration().blockchainRid,
                        arrayOf(), SECP256K1CryptoSystem())
                // sorting signatures makes it more likely we can avoid duplicate anchor transactions
                val sortedSignatures = witness.getSignatures().sortedBy { ByteArrayKey(it.subjectID) }
                txb.addOperation("anchor_block",
                        arrayOf(
                                GtvDecoder.decodeGtv(blockHeader),
                                GtvArray(
                                        sortedSignatures.map { GtvByteArray(it.subjectID) }.toTypedArray()
                                ),
                                GtvArray(
                                        sortedSignatures.map { GtvByteArray(it.data) }.toTypedArray()
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