package net.postchain.gtx

import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * These tests check the validation functions on operation arguments.
 */
class OpDataTest {

    // Max 64 bytes
    val okByteArray = ("00010203040506070809101112131415" +
            "00010203040506070809101112131415" +
            "00010203040506070809101112131415" +
            "00010203040506070809101112131415").hexStringToByteArray()
    val badByteArray = ("00010203040506070809101112131415" +
            "00010203040506070809101112131415" +
            "00010203040506070809101112131415" +
            "0001020304050607080910111213141500").hexStringToByteArray()

    // Max 64 chars
    val okString  = "1234567890123456789012345678901234567890123456789012345678901234"
    val badString = "12345678901234567890123456789012345678901234567890123456789012345"

    // Check we can handle big long values
    val okInt = Long.MAX_VALUE

    @Test
    fun happy() {
        val op = OpData("Olles op", arrayOf(gtv(okByteArray), gtv(okString), gtv(okInt), GtvNull))
        assertTrue(OpData.validateSimpleOperationArgs(op.args, op.opName))
    }

    @Test
    fun failTooBigByteArray() {
        val op = OpData("Olles op", arrayOf(gtv(badByteArray)))
        assertFalse(OpData.validateSimpleOperationArgs(op.args, op.opName))
    }

    @Test
    fun failTooBigString() {
        val op = OpData("Olles op", arrayOf(gtv(badString)))
        assertFalse(OpData.validateSimpleOperationArgs(op.args, op.opName))
    }

}