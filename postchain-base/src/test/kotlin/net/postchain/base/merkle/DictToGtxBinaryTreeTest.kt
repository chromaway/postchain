package net.postchain.base.merkle

import org.junit.Assert
import org.junit.Test

class DictToGtxBinaryTreeTest {

    @Test
    fun testIntDictLength4() {
        val treeHolderFromDict = GtxTreeDictHelper.buildThreeOf4_fromDict()
        //println(treeHolderFromDict.treePrintout)
        Assert.assertEquals(treeHolderFromDict.expectedPrintout.trim(), treeHolderFromDict.treePrintout.trim())
    }
}