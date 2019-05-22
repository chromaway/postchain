// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.gtx

import net.postchain.base.CryptoSystem
import net.postchain.base.merkle.Hash
import net.postchain.core.*
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder

/**
 * A transaction based on the GTX format.
 *
 * @property _rawData what the TX data looks like in binary form
 * @property txGtv what the TX data looks like in [Gtv]
 * @property rawGtx what the TX data looks like in GTX
 * @property signers are the public keys that should sign the TX
 * @property signatures are the actual signatures
 * @property ops are the operations of the TX
 * @property myHash is the merkle root of the TX
 * @property myRID  is the merkle root of the TX body
 * @property module is the module the transaction is related to
 * @property cs is the [CryptoSystem] we use
 */
class GTXTransaction (
        val _rawData: ByteArray?,
        val gtvData: Gtv,
        val gtxData: GTXTransactionData,
        val signers: Array<ByteArray>,
        val signatures: Array<ByteArray>,
        val ops: Array<Transactor>,
        val myHash: Hash,
        val myRID: ByteArray,
        module: GTXModule,
        val cs: CryptoSystem
): Transaction {

    var cachedRawData: ByteArray? = null // We are not sure we have the rawData, and if ever need to calculate it it will be cache here.
    var isChecked: Boolean = false

    override fun getHash(): ByteArray {
        return myHash
    }

    override fun isCorrect(): Boolean {
        if (isChecked) return true

        if (signatures.size != signers.size) return false

        for ((idx, signer) in signers.withIndex()) {
            val signature = signatures[idx]
            if (!cs.verifyDigest(myRID, Signature(signer, signature))) {
                return false
            }
        }

        for (op in ops) {
            if (!op.isCorrect()) return false
        }

        isChecked = true
        return true
    }

    @Synchronized
    override fun getRawData(): ByteArray {
        if (_rawData != null) {
            return _rawData
        }
        if (cachedRawData == null) {
            cachedRawData =  GtvEncoder.encodeGtv(gtvData)
        }
        return cachedRawData!!
    }

    override fun getRID(): ByteArray {
         return myRID
    }

    override fun apply(ctx: TxEContext): Boolean {
        if (!isCorrect()) throw UserMistake("Transaction is not correct")
        for (op in ops) {
            if (!op.apply(ctx))
                throw UserMistake("Operation failed")
        }
        return true
    }

}



