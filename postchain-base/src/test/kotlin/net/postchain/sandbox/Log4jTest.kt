package net.postchain.sandbox

import mu.KLogging
import org.junit.Ignore
import org.junit.Test

class Log4jTest {

    companion object : KLogging()

    @Test
    @Ignore
    fun test() {
        var i = 0
        while (true) {
            logger.error { "Hello Kitty - ${i++}" }
        }
    }

}