// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.common

import org.junit.Test
import kotlin.test.assertEquals

class ExponentialDelayTest {

    @Test
    fun happyTest() {
        val ed = ExponentialDelay(60000, 100)
        val first = ed.getDelayMillisAndIncrease()
        assertEquals(100L, first)

        val second = ed.getDelayMillisAndIncrease()
        assertEquals(200L, second)

        val third = ed.getDelayMillisAndIncrease()
        assertEquals(800L, third)

        for (i in 1..10) {
            ed.getDelayMillisAndIncrease()
        }

        val later = ed.getDelayMillisAndIncrease()
        assertEquals(60000L, later)


    }
}