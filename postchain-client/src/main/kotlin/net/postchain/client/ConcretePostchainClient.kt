package net.postchain.client.net.postchain.client

import net.postchain.client.*
import net.postchain.gtv.Gtv
import net.postchain.gtx.GTXDataBuilder
import nl.komponents.kovenant.Promise
import java.lang.Exception

class ConcretePostchainClient(val resolver: PostchainNodeResolver, val blockchainRID: ByteArray, val defaultSigner: DefaultSigner?) :PostchainClient
{
    override fun makeTransaction(signers: Array<ByteArray>): GTXTransactionBuilder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun postTransaction(b: GTXDataBuilder, confirmationLevel: ConfirmationLevel): Promise<TransactionResult, Exception> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun postTransactionSync(b: GTXDataBuilder, confirmationLevel: ConfirmationLevel): TransactionResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun query(name: String, args: List<Gtv>): Promise<Gtv, Exception> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun makeTransaction(): GTXTransactionBuilder {
        return GTXTransactionBuilder(this, blockchainRID, arrayOf(defaultSigner!!.pubkey))
    }

}