package net.postchain

import kotlin.test.Test
import kotlin.test.assertEquals

class BFTTest {

    @Test
    fun testGetBFTRequiredSignatureCount_1() {
        val result = getBFTRequiredSignatureCount(1)
        assertEquals(1, result)
    }

    @Test
    fun testGetBFTRequiredSignatureCount_2() {
        val result = getBFTRequiredSignatureCount(2)
        assertEquals(2, result)
    }

    @Test
    fun testGetBFTRequiredSignatureCount_3() {
        val result = getBFTRequiredSignatureCount(3)
        assertEquals(3, result)
    }

    @Test
    fun testGetBFTRequiredSignatureCount_4() {
        val result = getBFTRequiredSignatureCount(4)
        assertEquals(3, result)
    }

    @Test
    fun testGetBFTRequiredSignatureCount_5() {
        val result = getBFTRequiredSignatureCount(5)
        assertEquals(4, result)
    }

    @Test
    fun testGetBFTRequiredSignatureCount_6() {
        val result = getBFTRequiredSignatureCount(6)
        assertEquals(5, result)
    }

    @Test
    fun testGetBFTRequiredSignatureCount_7() {
        val result = getBFTRequiredSignatureCount(7)
        assertEquals(5, result)
    }

    @Test
    fun testGetBFTRequiredSignatureCount_100() {
        val result = getBFTRequiredSignatureCount(100)
        assertEquals(67, result)
    }
}