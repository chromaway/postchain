// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.controller

import com.google.gson.JsonObject
import net.postchain.api.rest.json.JsonFactory
import net.postchain.debug.NodeDiagnosticContext

interface DebugInfoQuery {
    /**
     * TODO: [POS-90]
     */
    fun queryDebugInfo(query: String?): String
}

class DefaultDebugInfoQuery(val nodeDiagnosticContext: NodeDiagnosticContext) : DebugInfoQuery {

    private val jsonBuilder = JsonFactory.makePrettyJson()

    override fun queryDebugInfo(query: String?): String {
        return when (query) {
            null -> collectDebugInfo()
            else -> unknownDebugInfoQuery(query)
        }
    }

    private fun collectDebugInfo(): String {
        return JsonObject()
                .apply {
                    nodeDiagnosticContext.getProperties().forEach { (property, value) ->
                        add(property, jsonBuilder.toJsonTree(value))
                    }
                }.let(jsonBuilder::toJson)
    }

    private fun unknownDebugInfoQuery(query: String): String {
        return JsonObject().apply {
            addProperty("unknown-debuginfo-query", query)
        }.toString()
    }
}