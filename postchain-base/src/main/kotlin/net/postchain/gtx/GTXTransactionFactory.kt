package net.postchain.gtx

import net.postchain.base.CryptoSystem
import net.postchain.core.Transaction
import net.postchain.core.TransactionFactory
import net.postchain.core.UserMistake
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash
import net.postchain.gtx.factory.GtxTransactionDataFactory
import net.postchain.gtx.serializer.GtxTransactionDataSerializer

/**
 * Idea is that we can build a [GTXTransaction] from different layers.
 * The most normal way would be to build from binary, but sometimes we might have deserialized the binary data already
 */
class GTXTransactionFactory(val blockchainRID: ByteArray, val module: GTXModule, val cs: CryptoSystem) : TransactionFactory {

    val gtvMerkleHashCalculator = GtvMerkleHashCalculator(cs) // Here we are using the standard cache

    override fun decodeTransaction(data: ByteArray): Transaction {
        return internalBuild(data)
    }

    fun build(gtvData: Gtv): GTXTransaction {
        return internalBuild(null, gtvData)
    }

    // Meant to be used in tests, could be deleted if not needed
    fun build(gtxData: GTXTransactionData): GTXTransaction {
        val gtvData = GtxTransactionDataSerializer.serializeToGtv(gtxData)
        return internalMainBuild(null, gtvData, gtxData)
    }

    // ----------------- Internal workings -------------------

    private fun internalBuild(rawData: ByteArray): GTXTransaction {
        val gtvData = GtvFactory.decodeGtv(rawData)
        return internalBuild(rawData, gtvData)
    }

    private fun internalBuild(rawData: ByteArray?, gtvData: Gtv): GTXTransaction {
        val gtxData = GtxTransactionDataFactory.deserializeFromGtv(gtvData)
        return internalMainBuild(rawData, gtvData, gtxData)
    }

    /**
     * Does the heavy lifting of creating the TX
     */
    private fun internalMainBuild(rawData: ByteArray?, gtvData: Gtv, gtxData: GTXTransactionData): GTXTransaction {

        val body = gtxData.transactionBodyData

        if (!body.blockchainRID.contentEquals(blockchainRID)) {
            throw UserMistake("Transaction has wrong blockchainRID")
        }

        // We wait until after validation before doing (expensive) merkle root calculation
        val myHash = gtvData.merkleHash(gtvMerkleHashCalculator)
        val myRID = body.calculateRID(gtvMerkleHashCalculator)

        // Extract some stuff
        val signers = body.signers
        val signatures = gtxData.signatures
        val ops = body.getExtOpData().map({ module.makeTransactor(it) }).toTypedArray()

        return GTXTransaction(rawData, gtvData, gtxData, signers, signatures, ops, myHash, myRID, module, cs)
    }

}