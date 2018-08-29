// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.api.rest.controller

import mu.KLogging
import net.postchain.api.rest.controller.HttpHelper.Companion.ACCESS_CONTROL_ALLOW_HEADERS
import net.postchain.api.rest.controller.HttpHelper.Companion.ACCESS_CONTROL_ALLOW_METHODS
import net.postchain.api.rest.controller.HttpHelper.Companion.ACCESS_CONTROL_ALLOW_ORIGIN
import net.postchain.api.rest.controller.HttpHelper.Companion.ACCESS_CONTROL_REQUEST_HEADERS
import net.postchain.api.rest.controller.HttpHelper.Companion.ACCESS_CONTROL_REQUEST_METHOD
import net.postchain.api.rest.json.JsonFactory
import net.postchain.api.rest.model.ApiTx
import net.postchain.api.rest.model.TxRID
import net.postchain.common.TimeLog
import net.postchain.common.hexStringToByteArray
import net.postchain.core.UserMistake
import spark.Request
import spark.Service

class RestApi(private val model: Model, private val listenPort: Int, private val basePath: String) {

    companion object : KLogging()

    private val http = Service.ignite()!!
    private val gson = JsonFactory.makeJson()

    init {
        buildRouter(http)
        logger.info { "Rest API listening on port ${actualPort()}" }
        logger.info { "Rest API attached on $basePath/" }
    }

    fun actualPort(): Int {
        return http.port()
    }

    private fun buildRouter(http: Service) {

        http.port(listenPort)

        http.exception(NotFoundError::class.java) { e, _, res ->
            logger.error("NotFoundError:", e)
            res.status(404)
            res.body(error(e))
        }

        http.exception(UserMistake::class.java) { e, _, res ->
            logger.error("UserMistake:", e)
            res.status(400)
            res.body(error(e))
        }

        http.exception(OverloadedException::class.java) { e, _, res ->
            res.status(503) // Service unavailable
            res.body(error(e))
        }

        http.exception(Exception::class.java) { e, _, res ->
            logger.error("Exception:", e)
            res.status(500)
            res.body(error(e))
        }

        http.notFound { _, _ -> error(UserMistake("Not found")) }

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

            http.post("/tx") { req, _ ->
                val n = TimeLog.startSumConc("RestApi.buildRouter().postTx")
                logger.debug("Request body: ${req.body()}")
                val tx = toTransaction(req)
                if (!tx.tx.matches(Regex("[0-9a-fA-F]{2,}"))) {
                    throw UserMistake("Invalid tx format. Expected {\"tx\": <hexString>}")
                }
                model.postTransaction(tx)
                TimeLog.end("RestApi.buildRouter().postTx", n)
                "{}"
            }

            http.get("/tx/:hashHex", "application/json", { req, _ ->
                val hashHex = checkHashHex(req)
                model.getTransaction(toTxRID(hashHex))
                        ?: throw NotFoundError("Can't find tx with hash $hashHex")
            }, gson::toJson)

            http.get("/tx/:hashHex/confirmationProof", { req, _ ->
                val hashHex = checkHashHex(req)
                model.getConfirmationProof(toTxRID(hashHex))
                        ?: throw NotFoundError("")
            }, gson::toJson)

            http.get("/tx/:hashHex/status", { req, _ ->
                val hashHex = checkHashHex(req)
                model.getStatus(toTxRID(hashHex))
            }, gson::toJson)

            http.post("/query") { req, _ ->
                handleQuery(req)
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

    private fun error(error: Exception): String {
        return gson.toJson(ErrorBody(error.message ?: "Unknown error"))
    }

    private fun handleQuery(req: Request): String {
        logger.debug("Request body: ${req.body()}")
        return model.query(Query(req.body())).json
    }

    private fun checkHashHex(req: Request): String {
        val hashHex = req.params(":hashHex")
        if (hashHex.length != 64 && !hashHex.matches(Regex("[0-9a-f]{64}"))) {
            throw NotFoundError("Invalid hashHex. Expected 64 hex digits [0-9a-f]")
        }
        return hashHex
    }

    fun stop() {
        http.stop()
        // Ugly hack to workaround that there is no blocking stop.
        // Test cases won't work correctly without it
        Thread.sleep(100)
    }
}
