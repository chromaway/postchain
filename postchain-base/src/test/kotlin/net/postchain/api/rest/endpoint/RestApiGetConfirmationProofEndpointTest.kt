// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.endpoint

import io.restassured.RestAssured.given
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.model.TxRID
import net.postchain.base.BaseBlockWitness
import net.postchain.base.ConfirmationProof
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree
import net.postchain.gtv.merkle.proof.ProofNodeGtvArrayHead
import net.postchain.gtv.merkle.proof.ProofValueGtvLeaf
import net.postchain.gtv.path.ArrayGtvPathElement
import org.easymock.EasyMock.*
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * [GetConfirmation] and [GetTx] endpoints have common part,
 * so see [RestApiGetTxEndpointTest] for additional tests
 */
class RestApiGetConfirmationProofEndpointTest {

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

    private fun buildDummyProof(): GtvMerkleProofTree {
        val gtv1 = gtv(1)
        val gtv2 = gtv(2)
        val gtvList = listOf(gtv1, gtv2)
        val rootProofElem = ProofNodeGtvArrayHead(
                gtvList.size,
                ProofValueGtvLeaf(gtv1, 0, ArrayGtvPathElement(null, 0)),
                ProofValueGtvLeaf(gtv2, 0, ArrayGtvPathElement(null, 1))
        )
        return GtvMerkleProofTree(rootProofElem)
    }

    @Test
    fun test_getConfirmationProof_ok() {
        val expectedObject = ConfirmationProof(
                txHashHex.toByteArray(),
                byteArrayOf(0x0a, 0x0b, 0x0c),
                BaseBlockWitness(
                        byteArrayOf(0x0b),
                        arrayOf()),
                buildDummyProof()
        )

        expect(model.getConfirmationProof(TxRID(txHashHex.hexStringToByteArray())))
                .andReturn(expectedObject)

        replay(model)

        restApi.attachModel(blockchainRID, model)

        given().basePath(basePath).port(restApi.actualPort())
                .get("/tx/$blockchainRID/$txHashHex/confirmationProof")
                .then()
                .statusCode(200)
                .body("hash", org.hamcrest.Matchers.not(isEmptyString()))
                .body("blockHeader", equalTo("0A0B0C"))
                .body("signatures.size()", equalTo(0))
                .body("merkleProofTree.size()", equalTo(5))

        verify(model)
    }
}