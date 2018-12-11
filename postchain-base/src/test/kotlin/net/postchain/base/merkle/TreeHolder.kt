package net.postchain.base.merkle

import net.postchain.gtx.GTXValue

open class TreeHolder(val orgIntArray: IntArray,
                 val orgGtxArray: ArrayList<GTXValue>,
                 val clfbTree: ContentLeafFullBinaryTree,
                 val treePrintout: String,
                 val expectedPrintout: String) {


}

class TreeHolderSubTree(orgIntArray: IntArray,
                        orgGtxArray: ArrayList<GTXValue>,
                        clfbTree: ContentLeafFullBinaryTree,
                        treePrintout: String,
                        expectedPrintout: String,
                        val orgGtxSubArray: Array<GTXValue>):
        TreeHolder (orgIntArray, orgGtxArray, clfbTree, treePrintout, expectedPrintout) {


}