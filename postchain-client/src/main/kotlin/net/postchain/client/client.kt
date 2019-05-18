package net.postchain.client

import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.gtv.Gtv
import net.postchain.gtx.GTXDataBuilder
import net.postchain.base.SigMaker
import net.postchain.core.TransactionStatus
import nl.komponents.kovenant.Promise
import java.lang.Exception

val cryptoSystem = SECP256K1CryptoSystem()

class GTXTransactionBuilder(val client: PostchainClient, blockchainRID: ByteArray, signers: Array<ByteArray>)
{
    val dataBuilder = GTXDataBuilder(blockchainRID, signers, cryptoSystem)
    fun addOperation(opName: String, args: Array<Gtv>) {
        dataBuilder.addOperation(opName, args)
    }

    fun sign(s: SigMaker) {
        if (!dataBuilder.finished) dataBuilder.finish()
        dataBuilder.addSignature(s.signDigest(dataBuilder.getDigestForSigning()))
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

    fun postTransaction(b: GTXDataBuilder, confirmationLevel: ConfirmationLevel): Promise<TransactionResult, Exception>
    fun postTransactionSync(b: GTXDataBuilder, confirmationLevel: ConfirmationLevel): TransactionResult

    fun query(name: String, args: List<Gtv>): Promise<Gtv, Exception>

}

interface PostchainNodeResolver {
    fun getNodeURL(blockchainRID: ByteArray): String
}

class DefaultSigner(val sigMaker: SigMaker, val pubkey: ByteArray) {
    companion object {
        fun makeDefaultSigner(privkey: ByteArray): DefaultSigner {
            TODO("")
        }
    }
}
