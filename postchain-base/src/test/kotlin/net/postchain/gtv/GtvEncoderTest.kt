package net.postchain.gtv

import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.toHex
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
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
        val size = 1024*1024 * 4 // that could make gtv size is around 27 MB
        val gtvArray  = (1..size).map { GtvInteger( it.toLong() ) }.toTypedArray()
        var encoded = ByteArray(0)
        val gtv = GtvArray(gtvArray)
        val serializationTime = measureTimeMillis {
            encoded = GtvEncoder.encodeGtv(gtv)
        }
        println("Size of gtv ~: ${encoded.size / (1024*1024)} MB")
        println("Execution time serialization: ${serializationTime} milliseconds")

        val deserializationTime = measureTimeMillis {
            GtvDecoder.decodeGtv(encoded).asArray()
        }
        println("Execution time deserialization: ${deserializationTime} milliseconds")

        val cs = SECP256K1CryptoSystem()
        val hashingTime = measureTimeMillis {
            val hash = gtv.merkleHash(GtvMerkleHashCalculator(cs))
            println(hash.toHex())
        }
        println("Execution hashing time: ${hashingTime} milliseconds")
    }
}