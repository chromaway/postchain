package net.postchain.gtv

import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import org.junit.Test
import java.math.BigInteger
import kotlin.system.measureTimeMillis
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
        val expected = GtvDictionary.build(map)
        val b = GtvEncoder.encodeGtv(expected)
        val result = GtvDecoder.decodeGtv(b)
        assertEquals(expected, result)
    }

    @Test
    fun stressTestGtv() {
        var hex = ""
        val size = 1024*1024*30 // 30mb

        // int array 30mb, we need to divide 4 because Int size is 4 bytes
        val gtvArray = Array<Gtv>(size/4) { GtvInteger(1L)}

        val executionTime = measureTimeMillis {
            hex = GtvEncoder.encodeGtv(GtvArray(gtvArray)).toHex()
        }
        println("Execution time serialization: ${executionTime} milliseconds")

        val executionTime2 = measureTimeMillis {
            val gtvArray = GtvDecoder.decodeGtv(hex.hexStringToByteArray()).asArray()
        }
        assertEquals(size/4, gtvArray.size)
        println("Execution time deserialization: ${executionTime2} milliseconds")

    }
}