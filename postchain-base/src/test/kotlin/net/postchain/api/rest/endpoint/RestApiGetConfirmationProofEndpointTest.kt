package net.postchain.api.rest.endpoint

import io.restassured.RestAssured
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.model.TxRID
import net.postchain.common.hexStringToByteArray
import org.easymock.EasyMock
import org.easymock.EasyMock.createMock
import org.easymock.EasyMock.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class RestApiGetConfirmationProofEndpointTest {

    private val basePath = "/api/v1"
    private lateinit var restApi: RestApi
    private lateinit var model: Model
    private val blockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3"
    private val txHashHex = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

    @Before
    fun setup() {
        model = createMock(Model::class.java)
        restApi = RestApi(0, basePath)
        restApi.attachModel(blockchainRID, model)
    }

    @After
    fun tearDown() {
        restApi.stop()
    }

    @Test
    fun test_getConfirmationProof_when_tx_does_not_exist_then_400_received() {
        EasyMock.expect(model.getConfirmationProof(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(null)
        EasyMock.replay(model)

        RestAssured.given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID/$txHashHex/confirmationProof")
                .then()
                .statusCode(404)

        verify(model)
    }
}