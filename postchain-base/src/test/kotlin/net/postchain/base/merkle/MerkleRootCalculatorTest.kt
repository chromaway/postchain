package net.postchain.base.merkle

import junit.framework.Assert.assertEquals
import org.junit.Test

class MerkleRootCalculatorTest {

    @Test
    fun testWith4Hashes() {
        //val calculator = MerkleHashCalculatorDummy()
        val treeHolder = HashTreeHelper.buildTreeOf4()


        //println(treeHolder.treePrintout)
        assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }
}