package net.postchain.base.merkle

import net.postchain.gtx.DictGTXValue
import net.postchain.gtx.GTXValue

open class TreeHolder(val orgIntArray: IntArray,
                      val clfbTree: ContentLeafFullBinaryTree,
                      val treePrintout: String,
                      val expectedPrintout: String) {


}

open class TreeHolderFromArray(orgIntArray: IntArray,
                          clfbTree: ContentLeafFullBinaryTree,
                          treePrintout: String,
                          expectedPrintout: String,
                          val orgGtxList: List<GTXValue> ):
        TreeHolder (orgIntArray, clfbTree, treePrintout, expectedPrintout) {
}

class TreeHolderSubTree(orgIntArray: IntArray,
                        clfbTree: ContentLeafFullBinaryTree,
                        treePrintout: String,
                        expectedPrintout: String,
                        orgGtxList: List<GTXValue>,
                        val orgGtxSubArray: Array<GTXValue>):
        TreeHolderFromArray (orgIntArray,  clfbTree, treePrintout, expectedPrintout, orgGtxList) {

}

class TreeHolderFromDict(orgIntArray: IntArray,
                         clfbTree: ContentLeafFullBinaryTree,
                         treePrintout: String,
                         expectedPrintout: String,
                         val orgGtxDict: DictGTXValue):
        TreeHolder (orgIntArray, clfbTree, treePrintout, expectedPrintout) {
}