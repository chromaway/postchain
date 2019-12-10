package net.postchain.common

import org.junit.Test
import kotlin.test.assertEquals

class ExponentialDelayTest {

    @Test
    fun happyTest() {
        val ed = ExponentialDelay(60000, 100)
        val first = ed.getDelayMillis()
        assertEquals(100L, first)

        val second = ed.getDelayMillis()
        assertEquals(200L, second)

        val third = ed.getDelayMillis()
        assertEquals(800L, third)

        for (i in 1..10) {
            ed.getDelayMillis()
        }

        val later = ed.getDelayMillis()
        assertEquals(60000L, later)


    }
}