package net.postchain.client.net.postchain.client

import net.postchain.client.*
import net.postchain.core.TransactionStatus
import net.postchain.gtv.Gtv
import net.postchain.gtx.GTXDataBuilder
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import java.lang.Exception
import org.spongycastle.crypto.tls.ConnectionEnd.client
import org.apache.http.client.methods.CloseableHttpResponse



class ConcretePostchainClient(val resolver: PostchainNodeResolver, val blockchainRID: ByteArray, val defaultSigner: DefaultSigner?) :PostchainClient
{
    override fun makeTransaction(signers: Array<ByteArray>): GTXTransactionBuilder {
        return GTXTransactionBuilder(this, blockchainRID, signers)
    }

    override fun postTransaction(builder: GTXDataBuilder, confirmationLevel: ConfirmationLevel): Promise<TransactionResult, Exception> {
        return task { doPostTransaction(builder, confirmationLevel) }
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

    private fun doPostTransaction(b: GTXDataBuilder, confirmationLevel: ConfirmationLevel) : TransactionResult {
        val serverUrl = resolver.getNodeURL(blockchainRID)
        when (confirmationLevel) {

            ConfirmationLevel.NO_WAIT -> {
                val httpClient = HttpClients.createDefault()
                val httpPost = HttpPost("${serverUrl}/tx/${blockchainRID}")
                httpPost.entity = StringEntity(String(b.serialize()))
                httpPost.setHeader("Accept", "application/json")
                httpPost.setHeader("Content-type", "application/json")
                val response = httpClient.execute(httpPost)
                if (response.statusLine.statusCode === 200) {
                    return UnknownTransaction()
                } else {
                    return RejectedTransaction()
                }
            }

            ConfirmationLevel.UNVERIFIED -> {

            }

            else -> {
                return RejectedTransaction()
            }
        }
    }

}