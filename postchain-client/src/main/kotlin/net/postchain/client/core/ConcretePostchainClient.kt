// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.client.core

import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import net.postchain.api.rest.json.JsonFactory
import net.postchain.base.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.TransactionStatus.*
import net.postchain.core.UserMistake
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXDataBuilder
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import java.io.BufferedReader
import java.io.InputStream

class ConcretePostchainClient(
        private val resolver: PostchainNodeResolver,
        private val blockchainRID: BlockchainRid,
        private val defaultSigner: DefaultSigner?
) : PostchainClient {

    private val gson = JsonFactory.makeJson()
    private val serverUrl = resolver.getNodeURL(blockchainRID)
    private val httpClient = HttpClients.createDefault()
    private val blockchainRIDHex = blockchainRID.toHex()
    private val retrieveTxStatusAttempts = 20
    private val retrieveTxStatusIntervalMs = 500L

    override fun makeTransaction(): GTXTransactionBuilder {
        return GTXTransactionBuilder(this, blockchainRID, arrayOf(defaultSigner!!.pubkey))
    }

    override fun makeTransaction(signers: Array<ByteArray>): GTXTransactionBuilder {
        return GTXTransactionBuilder(this, blockchainRID, signers)
    }

    override fun postTransaction(txBuilder: GTXDataBuilder, confirmationLevel: ConfirmationLevel): Promise<TransactionResult, Exception> {
        val def = deferred<TransactionResult, Exception>()
        try {
            def.resolve(doPostTransaction(txBuilder, confirmationLevel))
        } catch (e: Exception) {
            def.reject(e)
        }
        return def.promise
    }

    override fun postTransactionSync(txBuilder: GTXDataBuilder, confirmationLevel: ConfirmationLevel): TransactionResult {
        return doPostTransaction(txBuilder, confirmationLevel)
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

    private fun doQuery(name: String, gtv: Gtv): Gtv {
        val httpPost = HttpPost("$serverUrl/query_gtx/$blockchainRIDHex")
        val gtxQuery = gtv(gtv(name), gtv)
        val jsonQuery = """{"queries" : ["${GtvEncoder.encodeGtv(gtxQuery).toHex()}"]}""".trimMargin()
        with(httpPost) {
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

    private fun doPostTransaction(txBuilder: GTXDataBuilder, confirmationLevel: ConfirmationLevel): TransactionResult {
        val txHex = txBuilder.serialize().toHex()
        val txJson = """{"tx" : $txHex}"""
        val txHashHex = txBuilder.getDigestForSigning().toHex()

        fun submitTransaction(): CloseableHttpResponse {
            val httpPost = HttpPost("$serverUrl/tx/$blockchainRIDHex")
            httpPost.setHeader("Content-type", "application/json")
            httpPost.entity = StringEntity(txJson)
            return httpClient.execute(httpPost)
        }

        when (confirmationLevel) {

            ConfirmationLevel.NO_WAIT -> {
                val response = submitTransaction()
                return if (response.statusLine.statusCode == 200) {
                    TransactionResultImpl(WAITING)
                } else {
                    TransactionResultImpl(REJECTED)
                }
            }

            ConfirmationLevel.UNVERIFIED -> {
                submitTransaction()
                val httpGet = HttpGet("$serverUrl/tx/$blockchainRIDHex/$txHashHex/status")
                httpGet.setHeader("Content-type", "application/json")

                // keep polling till getting Confirmed or Rejected
                (0 until retrieveTxStatusAttempts).forEach { _ ->
                    try {
                        httpClient.execute(httpGet).entity?.let {
                            val response = parseResponse(it.content)
                            val jsonObject = gson.fromJson(response, JsonObject::class.java)
                            val status = valueOf(jsonObject.get("status").asString.toUpperCase())

                            if (status == CONFIRMED || status == REJECTED) {
                                return TransactionResultImpl(status)
                            }

                            Thread.sleep(retrieveTxStatusIntervalMs)
                        }
                    } catch (e: Exception) {
                        Thread.sleep(retrieveTxStatusIntervalMs)
                    }
                }

                return TransactionResultImpl(REJECTED)
            }

            else -> {
                return TransactionResultImpl(REJECTED)
            }
        }
    }

    private fun parseResponse(content: InputStream): String {
        return content.bufferedReader().use(BufferedReader::readText)
    }

}