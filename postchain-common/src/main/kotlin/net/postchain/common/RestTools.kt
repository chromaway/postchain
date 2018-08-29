// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.common

import io.restassured.RestAssured.given
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo

object RestTools {

    fun awaitConfirmed(port: Int, txRidHex: String) {
        await().untilCallTo {
            given().port(port)
                    .get("/tx/$txRidHex/status")
                    .then()
                    .statusCode(200)
                    .extract()
                    .body()

        } matches { body ->
            body?.jsonPath()?.getString("status") == "confirmed"
        }
    }
}
