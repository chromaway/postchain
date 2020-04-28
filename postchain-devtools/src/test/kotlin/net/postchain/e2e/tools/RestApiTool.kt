package net.postchain.e2e.tools

import io.restassured.RestAssured
import io.restassured.path.json.JsonPath

class RestApiTool(
        private val host: String,
        private val port: Int
) {

    fun getDebug(): JsonPath {
        return RestAssured
                .given()
                .baseUri("http://$host").port(port)

                .`when`()
                .get("/_debug")

                .then()
                .statusCode(200)

                .extract()
                .body()
                .jsonPath()
    }

}