package net.postchain.devtools.utils.configuration

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemSetupTest {


    val nodeNr1 = NodeSeqNumber(1)
    val nodeNr2 = NodeSeqNumber(2)
    val nodeNr3 = NodeSeqNumber(3)
    val nodeNr4 = NodeSeqNumber(4)
    val nodeNr5 = NodeSeqNumber(5)
    val nodeNr6 = NodeSeqNumber(6)

    val chainId1 = 1
    val chainId2 = 2
    val chainId3 = 3
    val chainId4 = 4

    val bcSetup1 = BlockchainSetup(chainId1, "111", listOf(nodeNr1, nodeNr2))
    val bcSetup2 = BlockchainSetup(chainId2, "222", listOf(nodeNr3, nodeNr4))
    val bcSetup3 = BlockchainSetup(chainId3, "333", listOf(nodeNr5), setOf(chainId1)) // chain3 depends on chain1
    val bcSetup4 = BlockchainSetup(chainId4, "444", listOf(nodeNr6), setOf(chainId3)) // chain4 depends on chain3


    fun init(): Map<Int, BlockchainSetup> {

        val blockchainMap: MutableMap<Int, BlockchainSetup> = mutableMapOf()
        blockchainMap[chainId1] = bcSetup1
        blockchainMap[chainId2] = bcSetup1
        blockchainMap[chainId3] = bcSetup1
        blockchainMap[chainId4] = bcSetup1
        return blockchainMap.toMap()
    }

    @Test
    fun checkNoDeps() {
        val blockchainMap = init()

        val foundSignerBcs = SystemSetup.calculateWhatBlockchainsTheNodeShouldSign(nodeNr1, blockchainMap)

        assertEquals(1, foundSignerBcs.size)
        val chainFound = foundSignerBcs.first()
        assertEquals(chainId1, chainFound)

        val foundReadBcs = SystemSetup.calculateWhatBlockchainsTheNodeShouldRead(blockchainMap[chainFound]!!.chainDependencies, blockchainMap)
        assertEquals(0, foundReadBcs.size)
    }

    @Test
    fun checkOneDep() {
        val blockchainMap = init()

        val foundSignerBcs = SystemSetup.calculateWhatBlockchainsTheNodeShouldSign(nodeNr5, blockchainMap)

        assertEquals(1, foundSignerBcs.size)
        val chainFound = foundSignerBcs.first()
        assertEquals(chainId3, chainFound)

        val foundReadBcs = SystemSetup.calculateWhatBlockchainsTheNodeShouldRead(blockchainMap[chainFound]!!.chainDependencies, blockchainMap)
        assertEquals(1, foundReadBcs.size)
        val depChainFound = foundReadBcs.first()
        assertEquals(chainId1, depChainFound)
    }


    @Test
    fun checkManyDeps() {
        val blockchainMap = init()

        val foundSignerBcs = SystemSetup.calculateWhatBlockchainsTheNodeShouldSign(nodeNr6, blockchainMap)

        assertEquals(1, foundSignerBcs.size)
        val chainFound = foundSignerBcs.first()
        assertEquals(chainId4, chainFound)

        val foundReadBcs = SystemSetup.calculateWhatBlockchainsTheNodeShouldRead(blockchainMap[chainFound]!!.chainDependencies, blockchainMap)
        assertEquals(2, foundReadBcs.size)
        assertTrue(foundReadBcs.contains(chainId1))
        assertTrue(foundReadBcs.contains(chainId3))
    }
}