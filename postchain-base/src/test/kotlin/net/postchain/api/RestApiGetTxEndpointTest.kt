package net.postchain.api

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
    private val hashHex = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

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
    fun test_getTx_Ok() {
        expect(model.getTransaction(TxRID(hashHex.hexStringToByteArray())))
                .andReturn(ApiTx("1234"))
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$hashHex")
                .then()
                .statusCode(200)
                .body("tx", equalTo("1234"))

        verify(model)
    }

    @Test
    fun test_getTx_when_slash_appended_Ok() {
        expect(model.getTransaction(TxRID(hashHex.hexStringToByteArray())))
                .andReturn(ApiTx("1234"))
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$hashHex")
                .then()
                .statusCode(200)
                .body("tx", equalTo("1234"))

        verify(model)
    }

    @Test
    fun test_getTx_when_not_found_then_404_received() {
        expect(model.getTransaction(TxRID(hashHex.hexStringToByteArray())))
                .andReturn(null)
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$hashHex")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_when_path_element_appended_then_404_received() {
        expect(model.getTransaction(TxRID(hashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$hashHex/element")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_when_hash_too_long_then_404_received() {
        expect(model.getTransaction(TxRID(hashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/${hashHex}0000")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_when_hash_too_short_then_404_received() {
        expect(model.getTransaction(TxRID(hashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/${hashHex.substring(1)}")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_when_hash_not_hex_then_400_received() {
        expect(model.getTransaction(TxRID(hashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/${hashHex.replaceFirst("a", "g")}")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_getTx_when_missing_hash_404_received() {
        expect(model.getTransaction(TxRID(hashHex.hexStringToByteArray())))
                .andReturn(null)
                .anyTimes()
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/")
                .then()
                .statusCode(404)

        verify(model)
    }

}