package net.postchain.gtv.merkle

import net.postchain.base.merkle.TreeHolder
import net.postchain.base.merkle.TreeHolderWithIntArray
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvArray
import net.postchain.gtv.Gtv


open class TreeHolderFromArray(orgIntArray: IntArray,
                               clfbTree:GtvBinaryTree,
                               treePrintout: String,
                               expectedPrintout: String,
                               val orgGtvArray: GtvArray  ):
        TreeHolderWithIntArray(orgIntArray, clfbTree, treePrintout, expectedPrintout)

class TreeHolderSubTree(orgIntArray: IntArray,
                        clfbTree:GtvBinaryTree,
                        treePrintout: String,
                        expectedPrintout: String,
                        orgSubGtvArray: GtvArray,
                        val orgGtvList: MutableList<Gtv>):
        TreeHolderFromArray (orgIntArray,  clfbTree, treePrintout, expectedPrintout, orgSubGtvArray)


class TreeHolderFromDict( clfbTree:GtvBinaryTree,
                         treePrintout: String,
                         expectedPrintout: String,
                         val orgGtvDict: GtvDictionary):
        TreeHolder(clfbTree, treePrintout, expectedPrintout)

