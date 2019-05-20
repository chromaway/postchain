package net.postchain.client.net.postchain.client

import net.postchain.api.rest.json.JsonFactory
import net.postchain.api.rest.model.ApiStatus
import net.postchain.client.*
import net.postchain.common.toHex
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
import org.apache.http.client.methods.HttpGet
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader


class ConcretePostchainClient(val resolver: PostchainNodeResolver, val blockchainRID: ByteArray, val defaultSigner: DefaultSigner?) :PostchainClient
{

    private val gson = JsonFactory.makeJson()

    override fun makeTransaction(signers: Array<ByteArray>): GTXTransactionBuilder {
        return GTXTransactionBuilder(this, blockchainRID, signers)
    }

    override fun postTransaction(builder: GTXDataBuilder, confirmationLevel: ConfirmationLevel): Promise<TransactionResult, Exception> {
        return task { doPostTransaction(builder, confirmationLevel) }
    }

    override fun postTransactionSync(builder: GTXDataBuilder, confirmationLevel: ConfirmationLevel): TransactionResult {
        return doPostTransaction(builder, confirmationLevel)
    }

    override fun query(name: String, args: List<Gtv>): Promise<Gtv, Exception> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun makeTransaction(): GTXTransactionBuilder {
        return GTXTransactionBuilder(this, blockchainRID, arrayOf(defaultSigner!!.pubkey))
    }

    private fun doPostTransaction(b: GTXDataBuilder, confirmationLevel: ConfirmationLevel) : TransactionResult {
        val serverUrl = resolver.getNodeURL(blockchainRID)
        val httpClient = HttpClients.createDefault()

        fun submitTransaction() : CloseableHttpResponse {
            val httpPost = HttpPost("${serverUrl}/tx/${blockchainRID}")
            httpPost.entity = StringEntity(String(b.serialize()))
            httpPost.setHeader("Accept", "application/json")
            httpPost.setHeader("Content-type", "application/json")
            return httpClient.execute(httpPost)
        }

        when (confirmationLevel) {
            
            ConfirmationLevel.NO_WAIT -> {
                val response = submitTransaction()
                if (response.statusLine.statusCode == 200) {
                    return UnknownTransaction()
                } else {
                    return RejectedTransaction()
                }
            }

            ConfirmationLevel.UNVERIFIED -> {
                val response = submitTransaction()
                val hashHex = b.serialize().toHex()
                val httpGet = HttpGet("${serverUrl}/tx/${blockchainRID}/${hashHex}/status")
                httpGet.setHeader("Content-type", "application/json")

                // keep polling till getting Confirmed or Rejected
                while (true) {
                    httpClient.execute(httpGet).entity?.let { e ->
                        val resp = parseResponse(e.content)
                        val status = gson.fromJson(resp, ApiStatus::class.java).status
                        when (status.toLowerCase()) {
                            "confirmed" -> {
                                return ConfirmedTransaction()
                            }

                            "rejected" -> {
                                return RejectedTransaction()
                            }

                            else -> {
                                Thread.sleep(500)
                            }
                        }
                    }
                }
            }

            else -> {
                return RejectedTransaction()
            }
        }
    }

    fun parseResponse(content: InputStream) : String {
        val bufferReader = BufferedReader(InputStreamReader(content))
        val ret : StringBuffer = StringBuffer()
        var line : String?
        line = bufferReader.readLine()
        while (line != null) {
            ret.append(line)
            line = bufferReader.readLine()
        }
        bufferReader.close()
        return ret.toString()
    }

}