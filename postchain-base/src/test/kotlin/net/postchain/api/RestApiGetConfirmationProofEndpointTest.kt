package net.postchain.api

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
    fun test_getConfirmationProof_when_Tx_Does_Not_Exist_then_400_received() {
        EasyMock.expect(model.getConfirmationProof(TxRID(hashHex.hexStringToByteArray())))
                .andReturn(null)
        EasyMock.replay(model)

        RestAssured.given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$hashHex/confirmationProof")
                .then()
                .statusCode(404)

        verify(model)
    }
}