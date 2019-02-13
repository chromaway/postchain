package net.postchain.gtv.merkle

import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.merkle.TreeHelper
import net.postchain.gtv.GtvInteger
import org.junit.Assert
import org.junit.Test

class GtvMerkleHashCalculatorTest {


    @Test
    fun testHashOfGtvCalculation_DummySerializaiton_DummyHash() {

        val calculator = MerkleHashCalculatorDummy()

        val iGtv = GtvInteger(7)
        // The "7" is expected to serialize to "07" (in hex)
        // The expected resulting leaf hash will be:
        val expectedResultAfterAddOneHash = "0108"
        // This expected result is two parts:
        // 1. "01" (= signals leaf) and
        // 2. the add-one-hashed value

        val result = calculator.calculateLeafHash(iGtv)
        Assert.assertEquals(expectedResultAfterAddOneHash, TreeHelper.convertToHex(result.getHashWithPrefix()))
    }


    @Test
    fun testHashOfGtvCalculation_RealSerialization_RealHash() {

        val calculator =GtvMerkleHashCalculator(SECP256K1CryptoSystem())

        val iGtv = GtvInteger(7)
        // The "7" is expected to serialize to "A303020107" (in hex)
        // The expected resulting leaf hash will be:
        val expectedResultAfterAddOneHash = "01616B6CDFF1A56CF63A0F04151C409B15941D5816D50DC739DBB4795D7BB97B6E"
        // This expected result is two parts:
        // 1. "01" (= signals leaf) and
        // 2. the 32 byte hashed value

        val result = calculator.calculateLeafHash(iGtv)
        Assert.assertEquals(expectedResultAfterAddOneHash, TreeHelper.convertToHex(result.getHashWithPrefix()))
    }
}