package net.postchain.api

import io.restassured.RestAssured.given
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.model.ApiTx
import net.postchain.common.toHex
import org.easymock.EasyMock.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class RestApiPostTxEndpointTest {

    private val basePath = "/api/v1"
    private lateinit var restApi: RestApi
    private lateinit var model: Model

    @Before
    fun setup() {
        model = createMock(Model::class.java)
        restApi = RestApi(model, 0, basePath)
    }

    @After
    fun tearDown() {
        restApi.stop()
    }

    @Test
    fun test_postTx_Ok() {
        val txHexString = "hello".toByteArray().toHex()
        model.postTransaction(ApiTx(txHexString))
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("{\"tx\": \"$txHexString\"}")
                .post("/tx")
                .then()
                .statusCode(200)

        verify(model)
    }

    @Test
    fun test_postTx_when_empty_message_then_400_received() {
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("")
                .post("/tx")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_missing_tx_property_then_400_received() {
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("{}")
                .post("/tx")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_empty_tx_property_then_400_received() {
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("{\"tx\": \"\"}")
                .post("/tx")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_tx_property_not_hex_then_400_received() {
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("{\"tx\": \"abc123z\"}")
                .post("/tx")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_invalid_json_then_400_received() {
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("a")
                .post("/tx")
                .then()
                .statusCode(400)

        verify(model)
    }

}