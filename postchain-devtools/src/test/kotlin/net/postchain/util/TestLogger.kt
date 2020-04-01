// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.util

import mu.KLogger
import mu.KLogging

/**
 * TestKLogging class provides [KLogger.log] method to log with default log level
 */
open class TestKLogging(private val defaultLevel: LogLevel) : KLogging() {

    enum class LogLevel {
        /*FATAL, */ERROR, WARN, INFO, DEBUG, TRACE
    }

    fun KLogger.log(msg: () -> Any?) {
        when (defaultLevel) {
//            LogLevel.FATAL -> logger
            LogLevel.ERROR -> error(msg)
            LogLevel.WARN -> warn(msg)
            LogLevel.INFO -> info(msg)
            LogLevel.DEBUG -> debug(msg)
            LogLevel.TRACE -> trace(msg)
        }
    }
}