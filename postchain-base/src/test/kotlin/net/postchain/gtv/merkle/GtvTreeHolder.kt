package net.postchain.gtv.merkle

import net.postchain.base.merkle.TreeHolder
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.Gtv


open class TreeHolderFromArray(orgIntArray: IntArray,
                               clfbTree:GtvBinaryTree,
                               treePrintout: String,
                               expectedPrintout: String,
                               val orgGtvList: List<Gtv> ):
        TreeHolder(orgIntArray, clfbTree, treePrintout, expectedPrintout) {
}

class TreeHolderSubTree(orgIntArray: IntArray,
                        clfbTree:GtvBinaryTree,
                        treePrintout: String,
                        expectedPrintout: String,
                        orgGtvList: List<Gtv>,
                        val orgGtvSubArray: Array<Gtv>):
        TreeHolderFromArray (orgIntArray,  clfbTree, treePrintout, expectedPrintout, orgGtvList) {

}

class TreeHolderFromDict(orgIntArray: IntArray,
                         clfbTree:GtvBinaryTree,
                         treePrintout: String,
                         expectedPrintout: String,
                         val orgGtvDict: GtvDictionary):
        TreeHolder(orgIntArray, clfbTree, treePrintout, expectedPrintout) {
}

