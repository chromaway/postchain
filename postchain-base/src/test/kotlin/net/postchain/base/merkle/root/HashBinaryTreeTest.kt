package net.postchain.base.merkle.root

import org.junit.Assert
import org.junit.Test

/**
 * This is so simple compared to the [GTXValue] stuff that one test is enough
 */
class HashBinaryTreeTest {


    @Test
    fun testStrArrayLength1() {
        val treeHolder = HashBinaryTreeHelper.buildTreeOf1()
        //println(treeHolder.treePrintout)
        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }

    @Test
    fun testStrArrayLength4() {
        val treeHolder = HashBinaryTreeHelper.buildTreeOf4()
        //println(treeHolder.treePrintout)
        Assert.assertEquals(treeHolder.expectedPrintout.trim(), treeHolder.treePrintout.trim())
    }


}