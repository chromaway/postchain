package net.postchain.common.logging

import org.slf4j.MDC

object LoggerContext {

    lateinit var contextMap: Map<String, String>

    fun init(contextMap_: Map<String, String>) {
        check(!::contextMap.isInitialized) {
            "LoggerContext is already initialized"
        }

        contextMap = contextMap_.toMap()
        MDC.setContextMap(contextMap_)
    }

    fun bind() {
        MDC.setContextMap(contextMap)
    }
}