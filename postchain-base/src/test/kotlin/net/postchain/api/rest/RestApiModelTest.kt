package net.postchain.api.rest

import com.google.gson.JsonParser
import io.restassured.RestAssured.given
import net.postchain.api.rest.controller.BlockHeight
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.json.JsonFactory
import net.postchain.api.rest.model.ApiTx
import net.postchain.api.rest.model.TxRID
import net.postchain.common.hexStringToByteArray
import net.postchain.core.BlockDetail
import net.postchain.core.PartialTx
import net.postchain.ebft.NodeState
import net.postchain.ebft.rest.contract.EBFTstateNodeStatusContract
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
                .assertThat().body(equalTo(JsonParser().parse("""{"error":"Not found"}""").toString()))

        verify(model)
    }


    @Test
    fun test_node_get_block_height() {
        restApi.attachModel(blockchainRID1, model)

        expect(model.nodeQuery("height"))
                .andReturn(gson.toJson(BlockHeight(42)))

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

        val response = EBFTstateNodeStatusContract(
                height = 233,
                serial = 41744989480,
                state = NodeState.WaitBlock,
                round = 0,
                revolting = false,
                blockRid = null
        )

        expect(model.nodeQuery("my_status"))
                .andReturn(gson.toJson(response))

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

        val response =
                arrayOf(
                        EBFTstateNodeStatusContract(
                                height = 233,
                                serial = 41744989480,
                                state = NodeState.WaitBlock,
                                round = 0,
                                revolting = false,
                                blockRid = null
                        ),
                        EBFTstateNodeStatusContract(
                                height = 233,
                                serial = 41744999981,
                                state = NodeState.WaitBlock,
                                round = 0,
                                revolting = false,
                                blockRid = null
                        ))

        expect(model.nodeQuery("statuses"))
                .andReturn(response.map { gson.toJson(it) }.toTypedArray().joinToString(separator = ",", prefix = "[", postfix = "]"))

        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/node/$blockchainRID1/statuses")
                .then()
                .statusCode(200)
                .assertThat().body(equalTo(gson.toJson(response).toString()))

      verify(model)
    }

    @Test
    fun test_blocks_get_all() {
        restApi.attachModel(blockchainRID1, model)
        val response = listOf<BlockDetail>(
                BlockDetail("blockRid001".toByteArray(), blockchainRID3.toByteArray(), "some header".toByteArray(), 0, listOf<ByteArray>(), listOf<PartialTx>(),"signatures".toByteArray(), 1574849700),
                BlockDetail("blockRid002".toByteArray(), "blockRid001".toByteArray(), "some other header".toByteArray(), 1, listOf<ByteArray>("tx1".toByteArray()), listOf<PartialTx>(),"signatures".toByteArray(),1574849760),
                BlockDetail("blockRid003".toByteArray(), "blockRid002".toByteArray(), "yet another header".toByteArray(), 2, listOf<ByteArray>(), listOf<PartialTx>(),"signatures".toByteArray(),1574849880),
                BlockDetail("blockRid004".toByteArray(), "blockRid003".toByteArray(), "guess what? Another header".toByteArray(), 3, listOf<ByteArray>("tx2".toByteArray(), "tx3".toByteArray(), "tx4".toByteArray()), listOf<PartialTx>(),"signatures".toByteArray(),1574849940)
        )
        expect(model.getBlocks(Long.MAX_VALUE,  false, 25, false))
                .andReturn(response)

        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/blocks/$blockchainRID1?before_block=${Long.MAX_VALUE}&limit=${25}&txs=true")
                .then()
                .statusCode(200)
                .assertThat().body(equalTo(gson.toJson(response).toString()))

        verify(model)
    }

    @Test
    fun test_blocks_get_last_2_partial() {
        restApi.attachModel(blockchainRID1, model)
        val response = listOf<BlockDetail>(
                BlockDetail("blockRid003".toByteArray(), "blockRid002".toByteArray(), "yet another header".toByteArray(), 2, listOf<ByteArray>(), listOf<PartialTx>(),"signatures".toByteArray(),1574849880),
                BlockDetail("blockRid004".toByteArray(), "blockRid003".toByteArray(), "guess what? Another header".toByteArray(), 3, listOf(), listOf<PartialTx >(PartialTx("hash2".toByteArray(), "tx2RID".toByteArray()), PartialTx("hash3".toByteArray(), "tx3RID".toByteArray()), PartialTx("hash4".toByteArray(), "tx4RID".toByteArray())), "signatures".toByteArray(),1574849940)
        )
        expect(model.getBlocks(Long.MAX_VALUE,  false, 2, true))
                .andReturn(response)

        replay(model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/blocks/$blockchainRID1?before_block=${3}&limit=${2}&txs=false")
                .then()
                .statusCode(200)
                .assertThat().body(equalTo(gson.toJson(response).toString()))

        verify(model)
    }
}
