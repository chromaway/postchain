// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.gtv

import net.postchain.base.BlockchainRid
import net.postchain.core.InitialBlockData
import net.postchain.gtv.*
import org.junit.Test
import kotlin.test.assertEquals

class BlockHeaderDataFactoryTest {

    val dummyBcRid = BlockchainRid.ZERO_RID
    val dummyPrevBlockRid = "b".toByteArray()
    val dummyBlockIID: Long = 1L
    val dummyChainID: Long = 2L
    val dummyHeight: Long = 3L
    val dummyTimestamp: Long = 4L

    val dummyRootHash = "aoeu".toByteArray()

    val dummyBlockRid = "z".toByteArray()

    @Test
    fun buildFromDomainObjects_4Dependencies() {

        val depArr = arrayOfNulls<ByteArray>(4)
        depArr[2] = dummyBlockRid
        val iBlockData = buildInitData(depArr)
        val timestamp = 12L
        val blockHeaderData = BlockHeaderDataFactory.buildFromDomainObjects(iBlockData, dummyRootHash, timestamp)

        val gtvDep = blockHeaderData.gtvDependencies
        assertEquals(GtvType.ARRAY, gtvDep.type)
        val gtvDepArr = gtvDep as GtvArray
        assertEquals(4, gtvDepArr.getSize())

        // Check the entire array
        assertEquals(GtvNull, gtvDepArr[0])
        assertEquals(GtvNull, gtvDepArr[1])
        assertEquals(GtvByteArray("z".toByteArray()), gtvDepArr[2])
        assertEquals(GtvNull, gtvDepArr[3])

    }

    @Test
    fun buildFromDomainObjects_emptyDependencies() {

        val iBlockData = buildInitData(null)
        val rootHash = "aoeu".toByteArray()
        val timestamp = 12L
        val blockHeaderData = BlockHeaderDataFactory.buildFromDomainObjects(iBlockData, rootHash, timestamp)

        assertEquals(blockHeaderData.gtvDependencies, GtvNull)
    }

    private fun buildInitData(blockHeightDependencyArr: Array<ByteArray?>?): InitialBlockData {
        return InitialBlockData(dummyBcRid, dummyBlockIID, dummyChainID, dummyPrevBlockRid, dummyHeight, dummyTimestamp, blockHeightDependencyArr)
    }

    @Test
    fun buildFromGtv_4Dependencies() {
        val depArr = arrayOf(GtvNull, GtvNull, GtvByteArray(dummyBlockRid), GtvNull)
        val gtvDependencies = GtvArray(depArr)
        val mainArr = buildGtvArray(gtvDependencies)
        val gtvMainArr = GtvArray(mainArr)
        val blockHeaderData = BlockHeaderDataFactory.buildFromGtv(gtvMainArr)

        val gtvDep = blockHeaderData.gtvDependencies
        assertEquals(GtvType.ARRAY, gtvDep.type)
        val gtvDepArr = gtvDep as GtvArray
        assertEquals(4, gtvDepArr.getSize())

        // Check the entire array
        assertEquals(GtvNull, gtvDepArr[0])
        assertEquals(GtvNull, gtvDepArr[1])
        assertEquals(GtvByteArray("z".toByteArray()), gtvDepArr[2])
        assertEquals(GtvNull, gtvDepArr[3])
    }

    @Test
    fun buildFromGtv_emptyDependencies() {
        val mainArr = buildGtvArray(GtvNull)
        val gtvMainArr = GtvArray(mainArr)
        val blockHeaderData = BlockHeaderDataFactory.buildFromGtv(gtvMainArr)

        assertEquals(blockHeaderData.gtvDependencies, GtvNull)
    }

    private fun buildGtvArray(dependencies: Gtv): Array<Gtv>  {

        val gtvBlockchainRid = GtvByteArray(dummyBcRid.data)
        val previousBlockRid = GtvByteArray(dummyPrevBlockRid)
        val merkleRootHash = GtvByteArray(dummyRootHash)
        val timestamp = GtvInteger(dummyTimestamp)
        val height = GtvInteger(dummyHeight)
        val extra= GtvDictionary.build(mapOf())

        return arrayOf(gtvBlockchainRid, previousBlockRid, merkleRootHash, timestamp, height, dependencies, extra)

    }
}