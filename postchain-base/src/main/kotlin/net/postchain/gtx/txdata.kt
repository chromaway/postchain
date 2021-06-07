// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx

import mu.KLogging
import net.postchain.base.BlockchainRid
import net.postchain.base.CryptoSystem
import net.postchain.base.SigMaker
import net.postchain.base.merkle.Hash
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.core.ProgrammerMistake
import net.postchain.core.Signature
import net.postchain.core.UserMistake
import net.postchain.gtv.*
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtx.factory.GtxTransactionDataFactory
import net.postchain.gtx.serializer.GtxTransactionBodyDataSerializer
import net.postchain.gtx.serializer.GtxTransactionDataSerializer
import java.lang.IllegalArgumentException
import java.util.*

object GtxBase {
    const val NR_FIELDS_TRANSACTION = 2
    const val NR_FIELDS_TRANSACTION_BODY = 3
    const val NR_FIELDS_OPERATION = 2
}

data class OpData(val opName: String, val args: Array<Gtv>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpData

        if (opName != other.opName) return false
        if (!Arrays.equals(args, other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = opName.hashCode()
        result = 31 * result + Arrays.hashCode(args)
        return result
    }
}

class ExtOpData(val opName: String,
                val opIndex: Int,
                val args: Array<Gtv>,
                val blockchainRID: BlockchainRid,
                val signers: Array<ByteArray>,
                val operations: Array<OpData> ) {

    companion object {

        /**
         * If we have [GTXTransactionBodyData] it will hold everything we need for extending the OpData
         */
        fun build(op: OpData, opIndex: Int, bodyData: GTXTransactionBodyData): ExtOpData {
            return ExtOpData(op.opName, opIndex, op.args, bodyData.blockchainRID, bodyData.signers, bodyData.operations)
        }
    }

}

val EMPTY_SIGNATURE: ByteArray = ByteArray(0)

data class GTXTransactionBodyData(
        val blockchainRID: BlockchainRid,
        val operations: Array<OpData>,
        val signers: Array<ByteArray>) {

    private var cachedRid: Hash? = null

    fun getExtOpData(): Array<ExtOpData> {
        return operations.mapIndexed { idx, op ->
            ExtOpData.build(op, idx, this)
        }.toTypedArray()
    }

    fun calculateRID(calculator: MerkleHashCalculator<Gtv>): Hash {
        if (cachedRid == null) {
            val txBodyGtvArr: GtvArray = GtxTransactionBodyDataSerializer.serializeToGtv(this)
            cachedRid = txBodyGtvArr.merkleHash(calculator)
        }

        return cachedRid!!
    }

    fun serialize(): ByteArray {
        val gtvArray =  GtxTransactionBodyDataSerializer.serializeToGtv(this)
        return GtvEncoder.encodeGtv(gtvArray)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GTXTransactionBodyData

        if (blockchainRID != other.blockchainRID) return false
        if (!Arrays.deepEquals(signers, other.signers)) return false
        if (!Arrays.equals(operations, other.operations)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blockchainRID.hashCode()
        result = 31 * result + Arrays.hashCode(signers)
        result = 31 * result + Arrays.hashCode(operations)
        return result
    }
}

data class GTXTransactionData(
        val transactionBodyData: GTXTransactionBodyData,
        val signatures: Array<ByteArray>) {

    fun serialize(): ByteArray {
        val gtvArray = GtxTransactionDataSerializer.serializeToGtv(this)
        return  GtvEncoder.encodeGtv(gtvArray)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GTXTransactionData

        if (transactionBodyData != other.transactionBodyData) return false
        if (!Arrays.deepEquals(signatures, other.signatures)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = transactionBodyData.hashCode()
        result = 31 * result + Arrays.hashCode(signatures)
        return result
    }
}


fun decodeGTXTransactionData(_rawData: ByteArray): GTXTransactionData {
    // Decode to RawGTV
    val gtv: Gtv = GtvDecoder.decodeGtv(_rawData)

    // GTV -> GTXTransactionData
    return GtxTransactionDataFactory.deserializeFromGtv(gtv)
}

// TODO: cache data for signing and digest

/**
 * Used for signing
 */
class GTXDataBuilder(val blockchainRID: BlockchainRid,
                     val signers: Array<ByteArray>,
                     val crypto: CryptoSystem,
                     val signatures: Array<ByteArray>,
                     val operations: MutableList<OpData>,
                     var finished: Boolean) {

    val calculator = GtvMerkleHashCalculator(crypto)

    // construct empty builder
    constructor(blockchainRID: BlockchainRid,
                signers: Array<ByteArray>,
                crypto: CryptoSystem) :
            this(
                    blockchainRID,
                    signers,
                    crypto,
                    Array(signers.size, { EMPTY_SIGNATURE }),
                    mutableListOf<OpData>(),
                    false)

    companion object {
        fun decode(bytes: ByteArray, crypto: CryptoSystem, finished: Boolean = true): GTXDataBuilder {
            val gtvData = GtvFactory.decodeGtv(bytes)
            val txData = GtxTransactionDataFactory.deserializeFromGtv(gtvData)
            val txBody = txData.transactionBodyData
            return GTXDataBuilder(
                    txBody.blockchainRID,
                    txBody.signers,
                    crypto,
                    txData.signatures,
                    txBody.operations.toMutableList(),
                    finished)
        }
    }

    fun finish() {
        finished = true
    }

    fun isFullySigned(): Boolean {
        return signatures.all { !it.contentEquals(EMPTY_SIGNATURE) }
    }

    fun addOperation(opName: String, args: Array<Gtv>) {
        if (finished) throw ProgrammerMistake("Already finished")
        operations.add(OpData(opName, args))
    }

    fun verifySignature(s: Signature): Boolean {
        return crypto.verifyDigest(getDigestForSigning(), s)
    }

    fun addSignature(s: Signature, check: Boolean = true) {
        if (!finished) throw ProgrammerMistake("Must be finished before signing")

        if (check) {
            if (!verifySignature(s)) {
                throw UserMistake("Signature is not valid")
            }
        }

        val idx = signers.indexOfFirst { it.contentEquals(s.subjectID) }
        if (idx != -1) {
            if (signatures[idx].contentEquals(EMPTY_SIGNATURE)) {
                signatures[idx] = s.data
            } else throw UserMistake("Signature already exists")
        } else throw UserMistake("Singer not found")
    }

    /**
     * @return Merkle root hash of transaction body
     */
    fun getDigestForSigning(): ByteArray {
        if (!finished) throw ProgrammerMistake("Must be finished before signing")

        return getGTXTransactionBodyData().calculateRID(calculator)
    }

    /**
     * @param sigMaker can create signatures
     * @return a signed merkle root of the TX body
     */
    fun sign(sigMaker: SigMaker) {
        addSignature(sigMaker.signDigest(getDigestForSigning()), false)
    }

    fun getGTXTransactionBodyData(): GTXTransactionBodyData {
        return GTXTransactionBodyData(
                blockchainRID,
                operations.toTypedArray(),
                signers)
    }

    fun getGTXTransactionData(): GTXTransactionData {
        val body = getGTXTransactionBodyData()
        return GTXTransactionData(body, signatures)
    }

    fun serialize(): ByteArray {
        return getGTXTransactionData().serialize()
    }
}
