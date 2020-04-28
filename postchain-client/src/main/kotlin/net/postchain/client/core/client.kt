// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.client.core

import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.SigMaker
import net.postchain.core.TransactionStatus
import net.postchain.gtv.Gtv
import net.postchain.gtx.GTXDataBuilder
import nl.komponents.kovenant.Promise

class GTXTransactionBuilder(private val client: PostchainClient, blockchainRID: BlockchainRid, signers: Array<ByteArray>) {

    private val dataBuilder = GTXDataBuilder(blockchainRID, signers, SECP256K1CryptoSystem())

    fun addOperation(opName: String, args: Array<Gtv>) {
        dataBuilder.addOperation(opName, args)
    }

    fun sign(sigMaker: SigMaker) {
        if (!dataBuilder.finished) {
            dataBuilder.finish()
        }
        dataBuilder.addSignature(sigMaker.signDigest(dataBuilder.getDigestForSigning()))
    }

    fun post(confirmationLevel: ConfirmationLevel): Promise<TransactionResult, Exception> {
        return client.postTransaction(dataBuilder, confirmationLevel)
    }

    fun postSync(confirmationLevel: ConfirmationLevel): TransactionResult {
        return client.postTransactionSync(dataBuilder, confirmationLevel)
    }
}

interface TransactionResult {
    val status: TransactionStatus
}

interface PostchainClient {
    fun makeTransaction(): GTXTransactionBuilder
    fun makeTransaction(signers: Array<ByteArray>): GTXTransactionBuilder

    fun postTransaction(txBuilder: GTXDataBuilder, confirmationLevel: ConfirmationLevel): Promise<TransactionResult, Exception>
    fun postTransactionSync(txBuilder: GTXDataBuilder, confirmationLevel: ConfirmationLevel): TransactionResult

    fun query(name: String, gtv: Gtv): Promise<Gtv, Exception>

}

interface PostchainNodeResolver {
    fun getNodeURL(blockchainRID: BlockchainRid): String
}

class DefaultSigner(val sigMaker: SigMaker, val pubkey: ByteArray)
