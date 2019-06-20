package net.postchain.common.logging

import java.io.File

class LoggerContextWrapper : ClassLoader() {

    private var cls: Class<*>
    private var loggerContext: Any

    private val CLASS_NAME = "LoggerContext"
    private val PACKAGE_NAME = "net.postchain.common.logging"
    private val PATH_TO_CLASS_FILE = "./target/classes/net/postchain/common/logging/LoggerContext.class"
    private val METHOD_INIT = "init"
    private val METHOD_BIND = "bind"

    init {
        val bytes = File(PATH_TO_CLASS_FILE).readBytes()
        cls = defineClass("$PACKAGE_NAME.$CLASS_NAME", bytes, 0, bytes.size)

        val constructor = cls.getDeclaredConstructor()
        constructor.isAccessible = true
        loggerContext = constructor.newInstance()
    }

    fun callInit(contextMap: Map<String, String>) {
        val method = cls.getDeclaredMethod(METHOD_INIT, Map::class.java)
        method.invoke(loggerContext, contextMap)
    }

    fun callBind() {
        val method = cls.getDeclaredMethod(METHOD_BIND)
        method.invoke(loggerContext)
    }
}