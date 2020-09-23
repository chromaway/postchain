// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.endpoint

import io.restassured.RestAssured.given
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRID
import net.postchain.common.hexStringToByteArray
import net.postchain.core.TransactionStatus
import org.easymock.EasyMock.*
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * [GetStatus] and [GetTx] endpoints have common part,
 * so see [RestApiGetTxEndpointTest] for additional tests
 */
class RestApiGetStatusEndpointTest {

    private val basePath = "/api/v1"
    private lateinit var restApi: RestApi
    private lateinit var model: Model
    private val blockchainRID = "ABABABABABABABABABABABABABABABABABABABABABABABABABABABABABABABAB"

    private val chainIid = 1L
    private val txHashHex = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

    @Before
    fun setup() {
        model = createMock(Model::class.java)
        expect(model.chainIID).andReturn(1L).anyTimes()

        restApi = RestApi(0, basePath, null, null)
    }

    @After
    fun tearDown() {
        restApi.stop()
    }

    @Test
    fun test_getStatus_ok() {
        expect(model.getStatus(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(ApiStatus(TransactionStatus.CONFIRMED))

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID/$txHashHex/status")
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("CONFIRMED"))

        verify(model)
    }

    @Test
    fun test_getStatus_ok_via_ChainIid() {
        expect(model.getStatus(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(ApiStatus(TransactionStatus.CONFIRMED))

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/iid_${chainIid.toInt().toString()}/$txHashHex/status")
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("CONFIRMED"))

        verify(model)
    }
}