package net.postchain.gtx.merkle

import net.postchain.base.merkle.TreeHolder
import net.postchain.gtx.DictGTXValue
import net.postchain.gtx.GTXValue


open class TreeHolderFromArray(orgIntArray: IntArray,
                               clfbTree: GtxBinaryTree,
                               treePrintout: String,
                               expectedPrintout: String,
                               val orgGtxList: List<GTXValue> ):
        TreeHolder(orgIntArray, clfbTree, treePrintout, expectedPrintout) {
}

class TreeHolderSubTree(orgIntArray: IntArray,
                        clfbTree: GtxBinaryTree,
                        treePrintout: String,
                        expectedPrintout: String,
                        orgGtxList: List<GTXValue>,
                        val orgGtxSubArray: Array<GTXValue>):
        TreeHolderFromArray (orgIntArray,  clfbTree, treePrintout, expectedPrintout, orgGtxList) {

}

class TreeHolderFromDict(orgIntArray: IntArray,
                         clfbTree: GtxBinaryTree,
                         treePrintout: String,
                         expectedPrintout: String,
                         val orgGtxDict: DictGTXValue):
        TreeHolder(orgIntArray, clfbTree, treePrintout, expectedPrintout) {
}