package net.postchain.api.rest

import io.restassured.RestAssured.given
import net.postchain.api.rest.contract.BlockHeight
import net.postchain.api.rest.contract.MyStatus
import net.postchain.api.rest.contract.NodeStatusContract
import net.postchain.api.rest.contract.NodeStatuses
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.json.JsonFactory
import net.postchain.api.rest.model.ApiTx
import net.postchain.api.rest.model.TxRID
import net.postchain.common.hexStringToByteArray
import net.postchain.ebft.NodeState
import org.easymock.EasyMock.*
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test

class RestApiModelTest {

    private val basePath = "/api/v1"
    private lateinit var restApi: RestApi
    private lateinit var model: Model
    private val blockchainRID1 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a1"
    private val blockchainRID2 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a2"
    private val blockchainRID3 = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3"
    private val blockchainRIDBadFormatted = "78967baa4768cbcef11c50"
    private val txRID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    private val gson = JsonFactory.makeJson()

    @Before
    fun setup() {
        model = createMock(Model::class.java)
        restApi = RestApi(0, basePath)
//        restApi.attachModel(blockchainRID, model)
    }

    @After
    fun tearDown() {
        restApi.stop()
    }

    @Test
    fun test_getTx_no_models_404_received() {
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID1/$txRID")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_unknown_model_404_received() {
        restApi.attachModel(blockchainRID1, model)
        restApi.attachModel(blockchainRID2, model)

        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID3/$txRID")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_case_insensitive_ok() {
        restApi.attachModel(blockchainRID1.toUpperCase(), model)

        expect(model.getTransaction(TxRID(txRID.hexStringToByteArray())))
                .andReturn(ApiTx("1234"))
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/${blockchainRID1.toLowerCase()}/$txRID")
                .then()
                .statusCode(200)

        verify(model)
    }

    @Test
    fun test_getTx_attach_then_detach_ok() {
        expect(model.getTransaction(TxRID(txRID.hexStringToByteArray())))
                .andReturn(ApiTx("1234")).times(1)
        replay(model)

        restApi.attachModel(blockchainRID1, model)
        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID1/$txRID")
                .then()
                .statusCode(200)

        restApi.detachModel(blockchainRID1)
        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID1/$txRID")
                .then()
                .statusCode(404)

        verify(model)
    }

    @Test
    fun test_getTx_incorrect_blockchainRID_format() {
        restApi.attachModel(blockchainRID1, model)
        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRIDBadFormatted/$txRID")
                .then()
                .statusCode(400)

        verify(model)
    }

    @Test
    fun test_node_get_block_height_null() {
        restApi.attachModel(blockchainRID1, model)

        expect(model.nodeQuery("height"))
                .andReturn(null)

        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/node/$blockchainRID1/height")
                .then()
                .statusCode(404)
                .assertThat().body(equalTo("""{"error":"NotFound"}"""))

        verify(model)
    }


    @Test
    fun test_node_get_block_height() {
        restApi.attachModel(blockchainRID1, model)

        expect(model.nodeQuery("height"))
                .andReturn(BlockHeight(42))

        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/node/$blockchainRID1/height")
                .then()
                .statusCode(200)
                .assertThat().body(equalTo("""{"blockHeight":42}"""))

        verify(model)
    }

    @Test
    fun test_node_get_my_status() {
        restApi.attachModel(blockchainRID1, model)

        val response = NodeStatusContract(
                height = 233,
                serial = 41744989480,
                state = NodeState.WaitBlock,
                round = 0,
                revolting = false,
                blockRid = null
        )

        expect(model.nodeQuery("my_status"))
                .andReturn(MyStatus(response))

        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/node/$blockchainRID1/my_status")
                .then()
                .statusCode(200)
                .assertThat().body(equalTo(gson.toJson(response).toString()))

        verify(model)
    }

    @Test
    fun test_node_get_statuses() {
        restApi.attachModel(blockchainRID1, model)

        val response: Array<NodeStatusContract> =
                arrayOf(
                        NodeStatusContract(
                                height = 233,
                                serial = 41744989480,
                                state = NodeState.WaitBlock,
                                round = 0,
                                revolting = false,
                                blockRid = null
                        ),
                        NodeStatusContract(
                                height = 233,
                                serial = 41744999981,
                                state = NodeState.WaitBlock,
                                round = 0,
                                revolting = false,
                                blockRid = null
                        ))

        expect(model.nodeQuery("statuses"))
                .andReturn(NodeStatuses(response))

        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/node/$blockchainRID1/statuses")
                .then()
                .statusCode(200)
                .assertThat().body(equalTo(gson.toJson(response).toString()))

      verify(model)
    }
}