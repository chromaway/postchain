// Copyright (c) 2020 ChromaWay AB. See README for license information.

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
        val expectedResultAfterAddOneHash = "0208"
        // This expected result is two parts:
        // 1. "01" (= signals leaf) and
        // 2. the add-one-hashed value

        val result = calculator.calculateLeafHash(iGtv)
        Assert.assertEquals(expectedResultAfterAddOneHash, TreeHelper.convertToHex(result))
    }


    @Test
    fun testHashOfGtvCalculation_RealSerialization_RealHash() {

        val calculator =GtvMerkleHashCalculator(SECP256K1CryptoSystem())

        val iGtv = GtvInteger(7)
        // The "7" is expected to serialize to "A303020107" (in hex)
        // The expected resulting leaf hash will be:
        val expectedResultAfterAddOneHash = "0140C7F79E11092AF89407F8F9A2A3230E3B92BE8398200AC00E8757BF1B9009"
        // This expected result is two parts:
        // 1. "01" (= signals leaf) and
        // 2. the 32 byte hashed value

        val result = calculator.calculateLeafHash(iGtv)
        Assert.assertEquals(expectedResultAfterAddOneHash, TreeHelper.convertToHex(result))
    }
}