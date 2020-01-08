package net.postchain.api.rest.endpoint

import com.nhaarman.mockitokotlin2.doReturn
import io.restassured.RestAssured.given
import net.postchain.api.rest.DummyConfig
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRID
import net.postchain.base.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer
import net.postchain.config.node.MockDatabaseConnector
import net.postchain.core.TransactionStatus
import org.easymock.EasyMock.*
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * [GetStatus] and [GetTx] endpoints have common part,
 * so see [RestApiGetTxEndpointTest] for additional tests
 */
@Ignore
class RestApiGetStatusEndpointTest {

    private val basePath = "/api/v1"
    private lateinit var restApi: RestApi
    private lateinit var model: Model
    private val blockchainRID = "ABABABABABABABABABABABABABABABABABABABABABABABABABABABABABABABAB"

    // Mock
    val mockAppConfigDbLayer: AppConfigDbLayer = com.nhaarman.mockitokotlin2.mock {
        on { getBlockchainRid(1L) } doReturn BlockchainRid.buildFromHex(blockchainRID)
    }
    private val chainIid = 1L
    private val txHashHex = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

    @Before
    fun setup() {
        model = createMock(Model::class.java)
        restApi = RestApi(
                0,
                basePath,
                AppConfig(DummyConfig.getDummyConfig()),
                null,
                null,
                { MockDatabaseConnector() },
                { _, _ -> mockAppConfigDbLayer }
        )
        restApi.attachModel(blockchainRID, model)
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

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/iid_${chainIid.toInt().toString()}/$txHashHex/status")
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("CONFIRMED"))

        verify(model)
    }
}