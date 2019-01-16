package net.postchain.base.merkle

import net.postchain.base.merkle.root.HashBinaryTree
import net.postchain.gtx.DictGTXValue
import net.postchain.gtx.GTXValue

open class TreeHolder(val orgIntArray: IntArray,
                      val clfbTree: GtxBinaryTree,
                      val treePrintout: String,
                      val expectedPrintout: String) {


}

open class TreeHolderFromArray(orgIntArray: IntArray,
                               clfbTree: GtxBinaryTree,
                               treePrintout: String,
                               expectedPrintout: String,
                               val orgGtxList: List<GTXValue> ):
        TreeHolder (orgIntArray, clfbTree, treePrintout, expectedPrintout) {
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
        TreeHolder (orgIntArray, clfbTree, treePrintout, expectedPrintout) {
}

open class TreeHashHolder(val orgIntArray: IntArray,
                          val clfbTree: HashBinaryTree,
                          val treePrintout: String,
                          val expectedPrintout: String) {
}

open class TreeHashHolderFromArray(orgIntArray: IntArray,
                                   clfbTree: HashBinaryTree,
                                   treePrintout: String,
                                   expectedPrintout: String,
                                   val orgHashList: List<Hash> ):
        TreeHashHolder (orgIntArray, clfbTree, treePrintout, expectedPrintout) {
}