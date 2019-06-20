package net.postchain.common.logging

import assertk.assertions.isEqualTo
import org.junit.Test
import org.slf4j.MDC
import java.lang.reflect.InvocationTargetException
import kotlin.concurrent.thread

class LoggerContextTest {

    @Test
    fun loggerContext_initialized_successfully() {
        val loggerContextWrapper = LoggerContextWrapper()
        val expected = mapOf(
                "k1" to "v1",
                "k2" to "v2"
        )
        loggerContextWrapper.callInit(expected)

        val actual = MDC.getCopyOfContextMap()
        assertk.assert(actual).isEqualTo(expected)
    }

    @Test
    fun secondInitialization_inTheSameThread_throws_exception() {
        val loggerContextWrapper = LoggerContextWrapper()
        loggerContextWrapper.callInit(mapOf())

        try {
            loggerContextWrapper.callInit(mapOf())
        } catch (e: InvocationTargetException) { // Since we use reflection for testing
            assertk.assert(e.cause is IllegalStateException).isEqualTo(true)
            assertk.assert(e.cause?.message).isEqualTo("LoggerContext is already initialized")
        }
    }

    @Test
    fun secondInitialization_inAnotherThread_throws_exception() {
        val loggerContextWrapper = LoggerContextWrapper()
        loggerContextWrapper.callInit(mapOf())

        var e: IllegalStateException? = null
        thread {
            try {
                loggerContextWrapper.callInit(mapOf())
            } catch (e_: InvocationTargetException) { // Since to we use reflection for testing
                e = e_.cause as? IllegalStateException
            }
        }.join()

        assertk.assert(e is IllegalStateException).isEqualTo(true)
        assertk.assert(e?.message).isEqualTo("LoggerContext is already initialized")
    }

    @Test
    fun loggerContextBinding_inTheSameThreadWhereInitialized_successful() {
        val loggerContextWrapper = LoggerContextWrapper()
        val expected = mapOf(
                "k1" to "v1",
                "k2" to "v2"
        )
        loggerContextWrapper.callInit(expected)
        loggerContextWrapper.callBind()

        val actual = MDC.getCopyOfContextMap()
        assertk.assert(actual).isEqualTo(expected)
    }

    @Test
    fun loggerContextBindingTwice_inTheSameThreadWhereInitialized_successful() {
        val loggerContextWrapper = LoggerContextWrapper()
        val expected = mapOf(
                "k1" to "v1",
                "k2" to "v2"
        )
        loggerContextWrapper.callInit(expected)
        loggerContextWrapper.callBind()
        loggerContextWrapper.callBind()

        val actual = MDC.getCopyOfContextMap()
        assertk.assert(actual).isEqualTo(expected)
    }

    @Test
    fun loggerContextBinding_inAnotherThread_successful() {
        val loggerContextWrapper = LoggerContextWrapper()
        val expected = mapOf(
                "k1" to "v1",
                "k2" to "v2"
        )
        loggerContextWrapper.callInit(expected)

        val actual = mutableMapOf<String, String>()
        thread {
            loggerContextWrapper.callBind()
            actual.putAll(MDC.getCopyOfContextMap())
        }.join()

        assertk.assert(actual).isEqualTo(expected)
    }

    @Test
    fun loggerContextBindingTwice_inAnotherThread_successful() {
        val loggerContextWrapper = LoggerContextWrapper()
        val expected = mapOf(
                "k1" to "v1",
                "k2" to "v2"
        )
        loggerContextWrapper.callInit(expected)

        val actual = mutableMapOf<String, String>()
        thread {
            loggerContextWrapper.callBind()
            loggerContextWrapper.callBind()
            actual.putAll(MDC.getCopyOfContextMap())
        }.join()

        assertk.assert(actual).isEqualTo(expected)
    }
}