// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockchainDependenciesTest {

    val dummyHash1 = BlockchainRid.buildRepeat(1)
    val dummyHash2 = BlockchainRid.buildRepeat(2)
    val dummyHash3 = BlockchainRid.buildRepeat(3)

    val dummyHash4 = "the4".toByteArray()
    val dummyHash5 = "the5".toByteArray()
    val dummyHash6 = "the6".toByteArray()

    @Test
    fun makeEmpty() {
        val bd = BlockchainDependencies(listOf())

        assertTrue(bd.isEmpty())
        assertEquals(bd.all().size, 0)
    }

    @Test
    fun make3_with_block_height() {
        val bd1 = BlockchainDependency(BlockchainRelatedInfo(dummyHash1, "dep1", 1), HeightDependency(dummyHash4, 2))
        val bd2 = BlockchainDependency(BlockchainRelatedInfo(dummyHash2, "dep2", 2), HeightDependency(dummyHash5, 7))
        val bd3 = BlockchainDependency(BlockchainRelatedInfo(dummyHash3, "dep3", 3), HeightDependency(dummyHash6, 9))
        val bd = BlockchainDependencies(listOf(bd1, bd2, bd3))

        assertEquals(bd.all().size, 3)

        // Make sure the order is correct (very important!)
        val retArr =bd.extractBlockHeightDependencyArray()!!
        assertTrue(retArr.contentEquals(arrayOf(dummyHash4, dummyHash5, dummyHash6)))

        // Try if lookup works
        assertEquals(bd.getFromBlockchainRID(dummyHash2)!!.heightDependency!!.height, 7)
        assertEquals(bd.getFromChainId(2L)!!.heightDependency!!.height, 7)
        assertNull(bd.getFromChainId(81L))

        // Try isDependingOnBlockchain
        assertTrue(bd.isDependingOnBlockchain(2))
        assertFalse(bd.isDependingOnBlockchain(881))
    }


    @Test
    fun make3_without_block_height() {
        val bd1 = BlockchainDependency(BlockchainRelatedInfo(dummyHash1, "dep1", 1), null)
        val bd2 = BlockchainDependency(BlockchainRelatedInfo(dummyHash2, "dep2", 2), null)
        val bd3 = BlockchainDependency(BlockchainRelatedInfo(dummyHash3, "dep3", 3), null)
        val bd = BlockchainDependencies(listOf(bd1, bd2, bd3))

        assertEquals(bd.all().size, 3)
        assertTrue( bd.extractBlockHeightDependencyArray()!!.contentEquals(arrayOfNulls(3)))

        // Lookup should not crash
        assertNull(bd.getFromBlockchainRID(dummyHash2)!!.heightDependency)
        assertNull(bd.getFromChainId(2L)!!.heightDependency)
    }

}