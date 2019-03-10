package net.postchain.gtv

import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class GtvEncoderTest {

    @Test
    fun testGtvNull() {
        val expected = GtvNull
        val b = GtvEncoder.encodeGtv(expected)
        val result = GtvDecoder.decodeGtv(b)
        assertEquals(expected, result)
    }

    @Test
    fun testGtvInteger() {
        val expected = GtvInteger(BigInteger.valueOf(Long.MAX_VALUE).pow(3))
        val b = GtvEncoder.encodeGtv(expected)
        val result = GtvDecoder.decodeGtv(b)
        assertEquals(expected, result)
        assertEquals(expected.asBigInteger().toString(10), result.asBigInteger().toString(10))
    }

    @Test
    fun testGtvString() {
        val expected = GtvString("postchain")
        val b = GtvEncoder.encodeGtv(expected)
        val result = GtvDecoder.decodeGtv(b)
        assertEquals(expected, result)
    }

    @Test
    fun testGtvByteArray() {
        var bytes =  ByteArray(3)
        bytes.set(0, 0x10)
        bytes.set(1, 0x1A)
        bytes.set(2, 0x68)
        val expected = GtvByteArray(bytes)
        val b = GtvEncoder.encodeGtv(expected)
        val result = GtvDecoder.decodeGtv(b)
        assertEquals(expected, result)
    }

    @Test
    fun testGtvArray() {
        val gtvArray = Array<Gtv>(3) { GtvString("postchain")}
        val expected = GtvArray(gtvArray)
        val b = GtvEncoder.encodeGtv(expected)
        val result = GtvDecoder.decodeGtv(b)
        assertEquals(expected, result)
    }

    @Test
    fun testGtvDictionary() {
        val map = mapOf(Pair("name", GtvString("postchain")))
        val expected = GtvDictionary(map)
        val b = GtvEncoder.encodeGtv(expected)
        val result = GtvDecoder.decodeGtv(b)
        assertEquals(expected, result)
    }
}