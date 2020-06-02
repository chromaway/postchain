// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.endpoint

import io.restassured.RestAssured.given
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.model.ApiTx
import net.postchain.api.rest.model.TxRID
import net.postchain.common.hexStringToByteArray
import org.easymock.EasyMock.*
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test

class RestApiGetTxEndpointTest {

    private val basePath = "/api/v1"
    private lateinit var restApi: RestApi
    private lateinit var model: Model
    private val blockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3"
    private val txHashHex = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

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
    fun test_getTx_Ok() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(ApiTx("1234"))

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID/$txHashHex")
                .then()
                .statusCode(200)
                .body("tx", equalTo("1234"))

        verify(model)
    }

    @Test
    fun test_getTx_when_slash_appended_Ok() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(ApiTx("1234"))

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID/$txHashHex")
                .then()
                .statusCode(200)
                .body("tx", equalTo("1234"))

        verify(model)
    }

    @Test
    fun test_getTx_when_not_found_then_404_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID/$txHashHex")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_when_path_element_appended_then_404_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID/$txHashHex/element")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_when_missing_blockchainRID_and_txHash_404_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_when_missing_txHash_404_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_when_missing_blockchainRID_404_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$txHashHex")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_when_blockchainRID_too_long_then_400_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/${blockchainRID}0000/$txHashHex")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_getTx_when_blockchainRID_too_short_then_400_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/${blockchainRID.substring(1)}/$txHashHex")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_getTx_when_blockchainRID_not_hex_then_400_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/${blockchainRID.replaceFirst("a", "g")}/$txHashHex")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_getTx_when_txHash_too_long_then_400_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID/${txHashHex}0000")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_getTx_when_txHash_too_short_then_400_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID/${txHashHex.substring(1)}")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_getTx_when_txHash_not_hex_then_400_received() {
        expect(model.getTransaction(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID/${txHashHex.replaceFirst("a", "g")}")
                .then()
                .statusCode(400)

        verify(model)
    }
}