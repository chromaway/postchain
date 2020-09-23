// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.endpoint

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
    private val blockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3"
    private lateinit var restApi: RestApi
    private lateinit var model: Model

    @Before
    fun setup() {
        model = createMock(Model::class.java)
        expect(model.chainIID).andReturn(1L).anyTimes()

        restApi = RestApi(0, basePath)
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

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("{\"tx\": \"$txHexString\"}")
                .post("/tx/$blockchainRID")
                .then()
                .statusCode(200)

        verify(model)
    }

    @Test
    fun test_postTx_when_empty_message_then_400_received() {
        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("")
                .post("/tx/$blockchainRID")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_missing_tx_property_then_400_received() {
        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("{}")
                .post("/tx/$blockchainRID")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_empty_tx_property_then_400_received() {
        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("{\"tx\": \"\"}")
                .post("/tx/$blockchainRID")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_tx_property_not_hex_then_400_received() {
        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("{\"tx\": \"abc123z\"}")
                .post("/tx/$blockchainRID")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_invalid_json_then_400_received() {
        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("a")
                .post("/tx/$blockchainRID")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_blockchainRID_too_long_then_400_received() {
        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("a")
                .post("/tx/${blockchainRID}0000")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_blockchainRID_too_short_then_400_received() {
        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("a")
                .post("/tx/${blockchainRID.substring(1)}")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_postTx_when_blockchainRID_not_hex_then_400_received() {
        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .body("a")
                .post("/tx/${blockchainRID.replaceFirst("a", "g")}")
                .then()
                .statusCode(400)

        verify(model)
    }
}