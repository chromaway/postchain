// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base

import org.junit.Test

import org.junit.Assert.*
import net.postchain.base.merkle.MerkleRootCalculator
import net.postchain.base.merkle.TreeHelper.convertToHex
import net.postchain.gtv.merkle.GtvMerkleHashCalculator

val cryptoSystem = SECP256K1CryptoSystem()
val merkleRootCalculator = MerkleRootCalculator(GtvMerkleHashCalculator(cryptoSystem))

class MerkleTest {


    fun stringToHash(string: String): ByteArray {
        return cryptoSystem.digest(string.toByteArray())
    }

    fun hashList(stringList: Array<String>): Array<ByteArray> {
        return stringList.map({stringToHash(it)}).toTypedArray()
    }

    fun merkleRoot(stringList: Array<String>): ByteArray {
        val hashList = hashList(stringList).toList()
        return merkleRootCalculator.calculateMerkleRoot(hashList)
        //return computeMerkleRootHash(cryptoSystem, hashList(stringList))
    }

    fun checkDifferent(list1: Array<String>, list2: Array<String>) {
        val root1 = merkleRoot(list1)
        val root2 = merkleRoot(list2)
        assertByteArrayNotEqual(root1, root2)
    }
    val a = arrayOf("a")
    val aa = arrayOf("a", "a")
    val abcde = arrayOf("a", "b", "c", "d", "e")
    val abcdee= arrayOf("a", "b", "c", "d", "e", "e")
    val abcdef = arrayOf("a", "b", "c", "d", "e", "f")
    val abcdefef = arrayOf("a", "b", "c", "d", "e", "f", "e", "f")

    fun assertByteArrayEqual(expected: ByteArray, actual: ByteArray) {
        assertTrue(expected.contentEquals(actual))
    }

    fun assertByteArrayNotEqual(val1: ByteArray, val2: ByteArray) {
        assertFalse(val1.contentEquals(val2))
    }

    /*
    @Test
    fun testMerkleRootOfEmptyListIs32Zeroes() {
        assertByteArrayEqual(kotlin.ByteArray(32), merkleRoot(emptyArray()))
    }

    @Test
    fun testMerkleRootOfSingleElement() {
        val merkleCalculation = merkleRoot(a)
        println("merkle: " + convertToHex(merkleCalculation))
        val expected =stringToHash("a")
        println("expected: " + convertToHex(expected))
        assertByteArrayEqual(expected, merkleCalculation)
    }

    @Test
    fun testMerkleRootNoCollisions() {
        checkDifferent(a, aa)
        checkDifferent(abcde, abcdee)
        checkDifferent(abcdef, abcdefef)
    }
    */

}