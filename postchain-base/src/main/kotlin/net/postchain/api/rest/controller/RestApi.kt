// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.api.rest.controller

import com.google.gson.*
import mu.KLogging
import net.postchain.api.rest.controller.HttpHelper.Companion.ACCESS_CONTROL_ALLOW_HEADERS
import net.postchain.api.rest.controller.HttpHelper.Companion.ACCESS_CONTROL_ALLOW_METHODS
import net.postchain.api.rest.controller.HttpHelper.Companion.ACCESS_CONTROL_ALLOW_ORIGIN
import net.postchain.api.rest.controller.HttpHelper.Companion.ACCESS_CONTROL_REQUEST_HEADERS
import net.postchain.api.rest.controller.HttpHelper.Companion.ACCESS_CONTROL_REQUEST_METHOD
import net.postchain.api.rest.controller.HttpHelper.Companion.PARAM_BLOCKCHAIN_RID
import net.postchain.api.rest.controller.HttpHelper.Companion.PARAM_HASH_HEX
import net.postchain.api.rest.controller.HttpHelper.Companion.SUBQUERY
import net.postchain.api.rest.json.JsonFactory
import net.postchain.api.rest.model.ApiTx
import net.postchain.api.rest.model.GTXQuery
import net.postchain.api.rest.model.TxRID
import net.postchain.common.TimeLog
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.UserMistake
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.*
import net.postchain.gtv.GtvFactory.gtv
import spark.QueryParamsMap
import spark.Request
import spark.Response
import spark.Service
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.reflect.full.safeCast

/**
 * Contains information on the rest API, such as network parameters and available queries
 */
class RestApi(
        private val listenPort: Int,
        private val basePath: String,
        private val sslCertificate: String? = null,
        private val sslCertificatePassword: String? = null
) : Modellable {

    val MAX_NUMBER_OF_BLOCKS_PER_REQUEST = 100
    val DEFAULT_BLOCK_HEIGHT_REQUEST = Long.MAX_VALUE
    val DEFAULT_ENTRY_RESULTS_REQUEST = 25
    val MAX_NUMBER_OF_TXS_PER_REQUEST = 600

    companion object : KLogging()

    private val http = Service.ignite()!!
    private val gson = JsonFactory.makeJson()
    private val models = mutableMapOf<String, Model>()

    init {
        buildErrorHandler(http)
        buildRouter(http)
        logger.info { "Rest API listening on port ${actualPort()}" }
        logger.info { "Rest API attached on $basePath/" }
    }

    override fun attachModel(blockchainRID: String, model: Model) {
        models[blockchainRID.toUpperCase()] = model
    }

    override fun detachModel(blockchainRID: String) {
        models.remove(blockchainRID.toUpperCase())
    }

    override fun retrieveModel(blockchainRID: String): Model? {
        return models[blockchainRID.toUpperCase()]
    }

    fun actualPort(): Int {
        return http.port()
    }

    private fun buildErrorHandler(http: Service) {
        http.exception(NotFoundError::class.java) { error, _, response ->
            logger.error("NotFoundError:", error)
            response.status(404)
            response.body(error(error))
        }

        http.exception(BadFormatError::class.java) { error, _, response ->
            logger.error("BadFormatError:", error)
            response.status(400)
            response.body(error(error))
        }

        http.exception(UserMistake::class.java) { error, _, response ->
            logger.error("UserMistake:", error)
            response.status(400)
            response.body(error(error))
        }

        http.exception(OverloadedException::class.java) { error, _, response ->
            response.status(503) // Service unavailable
            response.body(error(error))
        }

        http.exception(Exception::class.java) { error, _, response ->
            logger.error("Exception:", error)
            response.status(500)
            response.body(error(error))
        }

        http.notFound { _, _ -> error(UserMistake("Not found")) }
    }

    private fun buildRouter(http: Service) {

        http.port(listenPort)
        if (sslCertificate != null) {
            http.secure(sslCertificate, sslCertificatePassword, null, null)
        }
        http.before { req, res ->
            res.header(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
            res.header(ACCESS_CONTROL_REQUEST_METHOD, "POST, GET, OPTIONS")
            //res.header("Access-Control-Allow-Headers", "")
            res.type("application/json")

            // This is to provide compatibility with old postchain-client code
            req.pathInfo()
                    .takeIf { it.endsWith("/") }
                    ?.also { res.redirect(it.dropLast(1)) }
        }

        http.path(basePath) {

            http.options("/*") { request, response ->
                request.headers(ACCESS_CONTROL_REQUEST_HEADERS)?.let {
                    response.header(ACCESS_CONTROL_ALLOW_HEADERS, it)
                }
                request.headers(ACCESS_CONTROL_REQUEST_METHOD)?.let {
                    response.header(ACCESS_CONTROL_ALLOW_METHODS, it)
                }

                "OK"
            }

            http.post("/tx/$PARAM_BLOCKCHAIN_RID") { request, _ ->
                val n = TimeLog.startSumConc("RestApi.buildRouter().postTx")
                logger.debug("Request body: ${request.body()}")
                val tx = toTransaction(request)
                if (!tx.tx.matches(Regex("[0-9a-fA-F]{2,}"))) {
                    throw UserMistake("Invalid tx format. Expected {\"tx\": <hexString>}")
                }
                model(request).postTransaction(tx)
                TimeLog.end("RestApi.buildRouter().postTx", n)
                "{}"
            }

            http.get("/tx/$PARAM_BLOCKCHAIN_RID/$PARAM_HASH_HEX", "application/json", { request, _ ->
                runTxActionOnModel(request) { model, txRID ->
                    model.getTransaction(txRID)
                }
            }, gson::toJson)

            http.get("/transactions/$PARAM_BLOCKCHAIN_RID/$PARAM_HASH_HEX", "application/json", { request, _ ->
                  runTxActionOnModel(request) { model, txRID ->
                      model.getTransactionInfo(txRID)
                  }
            }, gson::toJson)
            http.get("/transactions/$PARAM_BLOCKCHAIN_RID", "application/json", { request, _ ->
                val model = model(request)
                val paramsMap = request.queryMap()
                val limit = paramsMap.get("limit")?.value()?.toIntOrNull()?.coerceIn(0, MAX_NUMBER_OF_TXS_PER_REQUEST) ?: DEFAULT_ENTRY_RESULTS_REQUEST
                val beforeTime = paramsMap.get("before-time")?.value()?.toLongOrNull() ?: Long.MAX_VALUE
                model.getTransactionsInfo(beforeTime, limit)
            }, gson::toJson)

            http.get("/tx/$PARAM_BLOCKCHAIN_RID/$PARAM_HASH_HEX/confirmationProof", { request, _ ->
                runTxActionOnModel(request) { model, txRID ->
                    model.getConfirmationProof(txRID)
                }
            }, gson::toJson)

            http.get("/tx/$PARAM_BLOCKCHAIN_RID/$PARAM_HASH_HEX/status", { request, _ ->
                runTxActionOnModel(request) { model, txRID ->
                    model.getStatus(txRID)
                }
            }, gson::toJson)

            http.get("/blocks/$PARAM_BLOCKCHAIN_RID", "application/json", { request, _ ->
                val model = model(request)
                val paramsMap = request.queryMap()
                val beforeTime = paramsMap.get("before-time")?.value()?.toLongOrNull() ?: Long.MAX_VALUE
                val limit = paramsMap.get("limit")?.value()?.toIntOrNull()?.coerceIn(0, MAX_NUMBER_OF_BLOCKS_PER_REQUEST) ?: DEFAULT_ENTRY_RESULTS_REQUEST
                val partialTxs = paramsMap.get("txs")?.value() != "true"
                model.getBlocks(beforeTime, limit, partialTxs)

            }, gson::toJson)

            http.get("/blocks/$PARAM_BLOCKCHAIN_RID/$PARAM_HASH_HEX", "applicatin/json", { request, _ ->
                val model = model(request)
                val blockRID = request.params(PARAM_HASH_HEX).hexStringToByteArray()
                val paramsMap = request.queryMap()
                val partialTx = paramsMap.get("txs").value() != "true"
                model.getBlock(blockRID, partialTx)
            }, gson::toJson)

            http.post("/query/$PARAM_BLOCKCHAIN_RID") { request, _ ->
                handleQuery(request)
            }

            http.post("/batch_query/$PARAM_BLOCKCHAIN_RID") { request, _ ->
                handleQueries(request)
            }

            // direct query. That should be used as example: <img src="http://node/dquery/brid?type=get_picture&id=4555" />
            http.get("/dquery/$PARAM_BLOCKCHAIN_RID") { request, response ->
                handleDirectQuery(request, response)
            }

            http.post("/query_gtx/$PARAM_BLOCKCHAIN_RID") { request, _ ->
                handleGTXQueries(request)
            }

            http.get("/node/$PARAM_BLOCKCHAIN_RID/$SUBQUERY", "application/json") { request, _ ->
                handleNodeStatusQueries(request)
            }
        }

        http.awaitInitialization()
    }

    private fun toTransaction(req: Request): ApiTx {
        try {
            return gson.fromJson<ApiTx>(req.body(), ApiTx::class.java)
        } catch (e: Exception) {
            throw UserMistake("Could not parse json", e)
        }
    }

    private fun toTxRID(hashHex: String): TxRID {
        val bytes: ByteArray
        try {
            bytes = hashHex.hexStringToByteArray()
        } catch (e: Exception) {
            throw UserMistake("Can't parse hashHex $hashHex", e)
        }

        val txRID: TxRID
        try {
            txRID = TxRID(bytes)
        } catch (e: Exception) {
            throw UserMistake("Bytes $hashHex is not a proper hash", e)
        }

        return txRID
    }

    private fun toGTXQuery(json: String): GTXQuery {
        try {
            val gson = Gson()
            return gson.fromJson<GTXQuery>(json, GTXQuery::class.java)
        } catch (e: Exception) {
            throw UserMistake("Could not parse json", e)
        }
    }

    private fun error(error: Exception): String {
        return gson.toJson(ErrorBody(error.message ?: "Unknown error"))
    }

    private fun handleQuery(request: Request): String {
        logger.debug("Request body: ${request.body()}")
        return model(request)
                .query(Query(request.body()))
                .json
    }

    private fun handleDirectQuery(request: Request, response: Response): Any {
        val queryMap = request.queryMap()
        val type = gtv(queryMap.value("type"))
        val args = GtvDictionary.build(queryMap.toMap().mapValues {
            gtv(queryMap.value(it.key))
        })
        val gtvQuery = GtvEncoder.encodeGtv(gtv(type, args))
        val array = model(request).query(GtvDecoder.decodeGtv(gtvQuery)).asArray()

        if (array.size < 2) {
            throw UserMistake("Response should have two parts: content-type and content")
        }
        // first element is content-type
        response.type(array[0].asString())
        val content = array[1]
        if (content.type == GtvType.STRING) {
            return content.asString()
        } else if (content.type == GtvType.BYTEARRAY) {
            return content.asByteArray()
        } else {
            throw UserMistake("Unexpected content")
        }
    }

    private fun handleQueries(request: Request): String {
        logger.debug("Request body: ${request.body()}")

        val queriesArray: JsonArray = parseMultipleQueriesRequest(request)

        var response: MutableList<String> = mutableListOf()

        queriesArray.forEach {
            var query = gson.toJson(it)
            response.add(model(request).query(Query(query)).json)
        }

        return gson.toJson(response)
    }

    private fun handleGTXQueries(request: Request): String {
        logger.debug("Request body: ${request.body()}")
        var response: MutableList<String> = mutableListOf<String>()
        val queriesArray: JsonArray = parseMultipleQueriesRequest(request)

        queriesArray.forEach {
            val hexQuery = it.asString
            val gtxQuery = GtvFactory.decodeGtv(hexQuery.hexStringToByteArray())
            response.add(GtvEncoder.encodeGtv(model(request).query(gtxQuery)).toHex())
        }

        return gson.toJson(response)
    }

    private fun handleNodeStatusQueries(request: Request): String {
        logger.debug("Request body: ${request.body()}")
        return model(request).nodeQuery(request.params(SUBQUERY))
    }

    private fun checkTxHashHex(request: Request): String {
        val hashHex = request.params(PARAM_HASH_HEX)
        if (!hashHex.matches(Regex("[0-9a-fA-F]{64}"))) {
            throw BadFormatError("Invalid hashHex. Expected 64 hex digits [0-9a-fA-F]")
        }
        return hashHex
    }

    private fun checkBlockchainRID(request: Request): String {
        val blockchainRID = request.params(PARAM_BLOCKCHAIN_RID)
        if (!blockchainRID.matches(Regex("[0-9a-fA-F]{64}"))) {
            throw BadFormatError("Invalid blockchainRID. Expected 64 hex digits [0-9a-fA-F]")
        }
        return blockchainRID
    }

    fun stop() {
        http.stop()
        // Ugly hack to workaround that there is no blocking stop.
        // Test cases won't work correctly without it
        Thread.sleep(100)
    }

    private fun runTxActionOnModel(request: Request, txAction: (Model, TxRID) -> Any?): Any? {
        val model = model(request)
        val txHashHex = checkTxHashHex(request)
        return txAction(model, toTxRID(txHashHex))
                ?: throw NotFoundError("Can't find tx with hash $txHashHex")
    }

    private fun model(request: Request): Model {
        val blockchainRID = checkBlockchainRID(request)
        return models[blockchainRID.toUpperCase()]
                ?: throw NotFoundError("Can't find blockchain with blockchainRID: $blockchainRID")
    }

    private fun parseMultipleQueriesRequest(request: Request): JsonArray {
        val element: JsonElement = gson.fromJson(request.body(), JsonElement::class.java)
        val jsonObject = element.asJsonObject
        return jsonObject.get("queries").asJsonArray
    }

}
