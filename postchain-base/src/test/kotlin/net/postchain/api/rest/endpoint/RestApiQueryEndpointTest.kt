// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.endpoint

import io.restassured.RestAssured
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.Query
import net.postchain.api.rest.controller.QueryResult
import net.postchain.api.rest.controller.RestApi
import net.postchain.core.ProgrammerMistake
import net.postchain.core.UserMistake
import org.easymock.EasyMock.*
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test

class RestApiQueryEndpointTest {

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
    fun test_query() {
        val queryString = """{"a"="b", "c"=3}"""
        val query = Query(queryString)

        val answerString = """{"d"=false}"""
        val answer = QueryResult(answerString)

        expect(model.query(query)).andReturn(answer)

        replay(model)

        restApi.attachModel(blockchainRID, model)

        RestAssured.given().basePath(basePath).port(restApi.actualPort())
                .body(queryString)
                .post("/query/$blockchainRID")
                .then()
                .statusCode(200)
                .body(equalTo(answerString))

        verify(model)
    }

    @Test
    fun test_query_UserError() {
        val queryString = """{"a"="b", "c"=3}"""
        val query = Query(queryString)

        val answerMessage = "expected error"
        val answerBody = """{"error":"expected error"}"""

        expect(model.query(query)).andThrow(
                UserMistake(answerMessage))

        replay(model)

        restApi.attachModel(blockchainRID, model)

        RestAssured.given().basePath(basePath).port(restApi.actualPort())
                .body(queryString)
                .post("/query/$blockchainRID")
                .then()
                .statusCode(400)
                .body(equalTo(answerBody))

        verify(model)
    }

    @Test
    fun test_query_other_error() {
        val queryString = """{"a"="b", "c"=3}"""
        val query = Query(queryString)

        val answerMessage = "expected error"
        val answerBody = """{"error":"expected error"}"""

        expect(model.query(query)).andThrow(ProgrammerMistake(answerMessage))

        replay(model)

        restApi.attachModel(blockchainRID, model)

        RestAssured.given().basePath(basePath).port(restApi.actualPort())
                .body(queryString)
                .post("/query/$blockchainRID")
                .then()
                .statusCode(500)
                .body(equalTo(answerBody))

        verify(model)
    }

    @Test
    fun test_query_when_blockchainRID_too_long_then_400_received() {
        val queryString = """{"a"="b", "c"=3}"""
        val answerBody = """{"error":"Invalid blockchainRID. Expected 64 hex digits [0-9a-fA-F]"}"""

        replay(model)

        restApi.attachModel(blockchainRID, model)

        RestAssured.given().basePath(basePath).port(restApi.actualPort())
                .body(queryString)
                .post("/query/${blockchainRID}0000")
                .then()
                .statusCode(400)
                .body(equalTo(answerBody))

        verify(model)
    }

    @Test
    fun test_query_when_blockchainRID_too_short_then_400_received() {
        val queryString = """{"a"="b", "c"=3}"""
        val answerBody = """{"error":"Invalid blockchainRID. Expected 64 hex digits [0-9a-fA-F]"}"""

        replay(model)

        restApi.attachModel(blockchainRID, model)

        RestAssured.given().basePath(basePath).port(restApi.actualPort())
                .body(queryString)
                .post("/query/${blockchainRID.substring(1)}")
                .then()
                .statusCode(400)
                .body(equalTo(answerBody))

        verify(model)
    }

    @Test
    fun test_query_when_blockchainRID_not_hex_then_400_received() {
        val queryString = """{"a"="b", "c"=3}"""
        val answerBody = """{"error":"Invalid blockchainRID. Expected 64 hex digits [0-9a-fA-F]"}"""

        replay(model)

        restApi.attachModel(blockchainRID, model)

        RestAssured.given().basePath(basePath).port(restApi.actualPort())
                .body(queryString)
                .post("/query/${blockchainRID.replaceFirst("a", "g")}")
                .then()
                .statusCode(400)
                .body(equalTo(answerBody))

        verify(model)
    }
}