package net.postchain.base.merkle

import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.gtx.IntegerGTXValue
import net.postchain.base.merkle.serializeGTXValueToByteArary
import net.postchain.base.merkle.dummyAddOneHashFun
import org.junit.Assert
import org.junit.Test

class MerkleHashCalculatorTest {


    @Test
    fun testHashOfGtxCalculation_DummySerializaiton_DummyHash() {

        val calculator = MerkleHashCalculatorDummy()

        val iGtx = IntegerGTXValue(7)
        // The "7" is expected to serialize to "07" (in hex)
        // The expected resulting leaf hash will be:
        val expectedResultAfterAddOneHash: String = "0108"
        // This expected result is two parts:
        // 1. "01" (= signals leaf) and
        // 2. the add-one-hashed value

        val result = calculator.calculateLeafHash(iGtx)
        Assert.assertEquals(expectedResultAfterAddOneHash, TreeHelper.convertToHex(result))
    }

    @Test
    fun testHashOfGtxCalculation_RealSerialization_DummyHash() {

        val calculator = MerkleHashCalculatorDummy()

        val iGtx = IntegerGTXValue(7)
        // The "7" is expected to serialize to "A303020107" (in hex)
        // The expected resulting leaf hash will be:
        val expectedResultAfterAddOneHash: String = "01A404030208"
        // This expected result is two parts:
        // 1. "01" (= signals leaf) and
        // 2. the add-one-hashed value

        val result = calculator.calculateHashOfGtxInternal(iGtx, ::serializeGTXValueToByteArary, ::dummyAddOneHashFun)
        Assert.assertEquals(expectedResultAfterAddOneHash, TreeHelper.convertToHex(result))
    }


    @Test
    fun testHashOfGtxCalculation_RealSerialization_RealHash() {

        val calculator = MerkleHashCalculatorBase(SECP256K1CryptoSystem())

        val iGtx = IntegerGTXValue(7)
        // The "7" is expected to serialize to "A303020107" (in hex)
        // The expected resulting leaf hash will be:
        val expectedResultAfterAddOneHash: String = "01616B6CDFF1A56CF63A0F04151C409B15941D5816D50DC739DBB4795D7BB97B6E"
        // This expected result is two parts:
        // 1. "01" (= signals leaf) and
        // 2. the 32 byte hashed value

        val result = calculator.calculateLeafHash(iGtx)
        Assert.assertEquals(expectedResultAfterAddOneHash, TreeHelper.convertToHex(result))
    }
}