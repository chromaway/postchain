// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.controller

class HttpHelper {

    companion object {
        const val ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin"
        const val ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method"
        const val ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods"
        const val ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers"
        const val ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers"

        const val PARAM_HASH_HEX = ":hashHex"
        const val SUBQUERY = ":subQuery"
        const val PARAM_BLOCKCHAIN_RID = ":blockchainRID"
        const val PARAM_UP_TO = ":upTo"
        const val PARAM_LIMIT = ":limit"
    }
}