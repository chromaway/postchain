package net.postchain.base.merkle

import net.postchain.gtv.merkle.GtvBinaryTree

open class TreeHolder(val clfbTree: GtvBinaryTree,
                      val treePrintout: String,
                      val expectedPrintout: String)

open class TreeHolderWithIntArray(
        val orgIntArray: IntArray,
        clfbTree:GtvBinaryTree,
        treePrintout: String,
        expectedPrintout: String
): TreeHolder(clfbTree, treePrintout, expectedPrintout)



