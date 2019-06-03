package net.postchain.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import net.postchain.api.rest.json.JsonFactory
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.TransactionStatus
import net.postchain.core.UserMistake
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvFactory
import net.postchain.gtx.GTXDataBuilder
import nl.komponents.kovenant.Promise
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import java.lang.Exception
import net.postchain.gtv.GtvFactory.gtv
import nl.komponents.kovenant.deferred
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ConcretePostchainClient(val resolver: PostchainNodeResolver, val blockchainRID: ByteArray, val defaultSigner: DefaultSigner?) :PostchainClient
{

    private val gson = JsonFactory.makeJson()
    private val serverUrl = resolver.getNodeURL(blockchainRID)
    private val httpClient = HttpClients.createDefault()
    private val blockchainRIDHex = blockchainRID.toHex()

    override fun makeTransaction(signers: Array<ByteArray>): GTXTransactionBuilder {
        return GTXTransactionBuilder(this, blockchainRID, signers)
    }

    override fun postTransaction(builder: GTXDataBuilder, confirmationLevel: ConfirmationLevel): Promise<TransactionResult, Exception> {
        val def = deferred<TransactionResult, Exception>()
        try {
            def.resolve(doPostTransaction(builder, confirmationLevel))
        } catch (e: Exception) {
            def.reject(e)
        }
        return def.promise
    }

    override fun postTransactionSync(builder: GTXDataBuilder, confirmationLevel: ConfirmationLevel): TransactionResult {
        return doPostTransaction(builder, confirmationLevel)
    }

    override fun query(name: String, gtv: Gtv): Promise<Gtv, Exception> {
        val def = deferred<Gtv, Exception>()
        try {
            def.resolve(doQuery(name, gtv))
        } catch (e: Exception) {
            def.reject(e)
        }
        return def.promise
    }

    override fun makeTransaction(): GTXTransactionBuilder {
        return GTXTransactionBuilder(this, blockchainRID, arrayOf(defaultSigner!!.pubkey))
    }

    private fun doQuery(name: String, gtv : Gtv) : Gtv {
        val httpPost = HttpPost("${serverUrl}/query_gtx/${blockchainRIDHex}")
        val gtxQuery = gtv(gtv(name), gtv)
        val jsonQuery = """{"queries" : ["${GtvEncoder.encodeGtv(gtxQuery).toHex()}"]}""".trimMargin()
        with (httpPost) {
            entity = StringEntity(jsonQuery)
            setHeader("Accept", "application/json")
            setHeader("Content-type", "application/json")
        }
        val response = httpClient.execute(httpPost)
        if (response.statusLine.statusCode != 200) {
            throw UserMistake("Can not make query_gtx api call ")
        }
        val type = object : TypeToken<List<String>>() {}.type
        val gtxHexCode = gson.fromJson<List<String>>(parseResponse(response.entity.content), type)?.first()
        return GtvFactory.decodeGtv(gtxHexCode!!.hexStringToByteArray())
    }

    private fun doPostTransaction(b: GTXDataBuilder, confirmationLevel: ConfirmationLevel) : TransactionResult {
        val txHex = b.serialize().toHex()
        val txJson = """{"tx" : ${txHex}}"""
        val txHashHex = b.getDigestForSigning().toHex()

        fun submitTransaction() : CloseableHttpResponse {
            val httpPost = HttpPost("${serverUrl}/tx/${blockchainRIDHex}")
            httpPost.setHeader("Content-type", "application/json")
            httpPost.entity = StringEntity(txJson)
            return httpClient.execute(httpPost)
        }

        when (confirmationLevel) {

            ConfirmationLevel.NO_WAIT -> {
                val response = submitTransaction()
                if (response.statusLine.statusCode == 200) {
                    return TransactionResultImpl(TransactionStatus.WAITING)
                } else {
                    return TransactionResultImpl(TransactionStatus.REJECTED)
                }
            }

            ConfirmationLevel.UNVERIFIED -> {
                submitTransaction()
                val httpGet = HttpGet("${serverUrl}/tx/${blockchainRIDHex}/${txHashHex}/status")
                httpGet.setHeader("Content-type", "application/json")

                // keep polling till getting Confirmed or Rejected
                while (true) {
                    httpClient.execute(httpGet).entity?.let { e ->
                        val resp = parseResponse(e.content)
                        val jsonObject = Gson().fromJson(resp, JsonObject::class.java)
                        val status = jsonObject.get("status").asString
                        println("Status: ${status}")
                        when (status.toLowerCase()) {
                            "confirmed" -> {
                                return TransactionResultImpl(TransactionStatus.CONFIRMED)
                            }

                            "rejected" -> {
                                return TransactionResultImpl(TransactionStatus.REJECTED)
                            }

                            else -> {
                                Thread.sleep(500)
                            }
                        }
                    }
                }
            }

            else -> {
                return TransactionResultImpl(TransactionStatus.REJECTED)
            }
        }
    }

    private fun parseResponse(content: InputStream) : String {
        val bufferReader = BufferedReader(InputStreamReader(content))
        val ret = StringBuffer()
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