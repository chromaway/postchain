package net.postchain.e2e.tools

import io.restassured.path.json.JsonPath

class CurlTool(
        private val container: KGenericContainer,
        private val port: Int
) {

    fun getDebug(): JsonPath {
        return container
                .execInContainer("curl", "http://localhost:$port/_debug")
                .run { JsonPath.from(stdout) }
    }
}