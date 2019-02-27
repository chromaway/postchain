package net.postchain.gtv.merkle.virtual

import net.postchain.base.merkle.proof.MerkleProofElement
import net.postchain.base.merkle.proof.ProofHashedLeaf
import net.postchain.base.merkle.proof.ProofNode
import net.postchain.base.merkle.proof.ProofValueLeaf
import net.postchain.core.UserMistake
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvVirtual
import net.postchain.gtv.GtvVirtualArray
import net.postchain.gtv.GtvVirtualDictionary
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree
import net.postchain.gtv.merkle.proof.ProofNodeGtvArrayHead
import net.postchain.gtv.merkle.proof.ProofNodeGtvDictHead
import net.postchain.gtv.merkle.proof.ProofValueGtvLeaf
import net.postchain.gtv.path.DictGtvPathElement


/**
 * This factory can build [GtvVirtual] structures out of proof trees
 */
object GtvVirtualFactory {

    /**
     * Turns a proof tree into a virtual GTV.
     *
     * Note: There is this strange corner case where the prof in the ENTIRE structure (i.e. the proof tree and the
     * original source GTV object graph are the same). I have chosen not to handle trivial cases, the
     * result would be null anyway.
     *
     * @return a [GtvVirtual] that corresponds to the original [Gtv]
     */
    fun buildGtvVirtual(proofTree: GtvMerkleProofTree): GtvVirtual {
        return buildGtvVirtualInner(proofTree.root)
    }

    private fun buildGtvVirtualInner(currentElement: MerkleProofElement): GtvVirtual {
        return when (currentElement) {
            is ProofNodeGtvArrayHead -> buildGtvVirtualArray(currentElement)
            is ProofNodeGtvDictHead -> buildGtvVirtualDictionary(currentElement)
            is ProofNode ->         throw UserMistake("A proof structure cannot have an (internal) node as root.")
            is ProofHashedLeaf ->   throw UserMistake("A proof structure cannot have a hash as root.")
            is ProofValueLeaf<*> -> throw UserMistake("A proof structure cannot be just the value that should be proven (meaningless).") // TODO: Maybe this shoud change?
            else ->                 throw UserMistake("We don't handle proofs that begin with type: ${currentElement::class.simpleName}")
        }
    }

    // --------------------- Array ----------------------

    /**
     * We transform an entire binary tree structure - that came form a [GtvArray] - back to an [GtvVirtualArray].
     *
     * [ProofNode] are meaningless, because didn't exist in the original array.
     * [ProofHashedLeaf] are also of no interest, since we don't know what the original value was.
     *
     * @param arrHeadElement the proof node that corresponds to the head of the original [GtvArray]
     */
    fun buildGtvVirtualArray(arrHeadElement: ProofNodeGtvArrayHead): GtvVirtualArray {
        val tmpSet = buildGtvVirtualArrayInner(arrHeadElement)
        return tmpSet.buildGtvVirtualArray(arrHeadElement.size)
    }

    private fun buildGtvVirtualArrayInner(currentElement: MerkleProofElement): ArrayIndexAndGtvSet {
        return when(currentElement) {
            is ProofHashedLeaf       -> ArrayIndexAndGtvSet()                                          // Empty leaf. We don't care about these
            is ProofValueGtvLeaf     -> ArrayIndexAndGtvSet(currentElement.content)                    // Valuable leaf (at a leaf that holds a real value [Gtv])
            is ProofNodeGtvDictHead  -> ArrayIndexAndGtvSet(buildGtvVirtualDictionary(currentElement)) // Valuable leaf (at the the beginning of a new dict-tree)
            is ProofNodeGtvArrayHead -> ArrayIndexAndGtvSet(buildGtvVirtualArray(currentElement))      // Valuable leaf (at the the beginning of a new array-tree)
            is ProofNode -> {                                                                          // Meaningless intermediary, should be removed.
                val leftSet = buildGtvVirtualArrayInner(currentElement.left)
                val rightSet = buildGtvVirtualArrayInner(currentElement.right)
                leftSet.addAll(rightSet)
                leftSet
            }
            else -> {
                throw IllegalStateException("Should have handled this type: $currentElement")
            }
        }
    }

    /**
     * TODO:POS-8 Delete this if not used after 2019-06-01
     *
     * If we are at a:
     * 1. leaf we return a set with one element in it.
     * 2. node we calculate left and right side, and smash the results together
     *
     *
     *
    private fun buildGtvVirtualArrayInner(currentElement: MerkleProofElement): ArrayIndexAndGtvSet {
        return when (currentElement) {
            is ProofHashedLeaf -> ArrayIndexAndGtvSet() // We are not interested in these, just return empty set.
            is ProofValueLeaf<*> -> ArrayIndexAndGtvSet(currentElement.content as Gtv) // We are at a leaf that holds a real value [Gtv]. We should preserve this value
            is ProofNodeGtvDictHead -> {
                // We are at a leaf, that happens to be the beginning of a new tree
                val virtualDict = buildGtvVirtualDictionary(currentElement)
                ArrayIndexAndGtvSet(virtualDict)
            }
            is ProofNodeGtvArrayHead -> {
                // We are at a leaf, that happens to be the beginning of a new tree
                val virtualArr = buildGtvVirtualArray(currentElement)
                ArrayIndexAndGtvSet(virtualArr)
            }
            is ProofNode -> {
                // This is just an internal node and should be removed, only thing to remember here is to keep track
                // of the position.
                val leftSet = buildGtvVirtualArrayInner(currentElement.left)
                leftSet.updateAllHeights()

                val rightSet = buildGtvVirtualArrayInner(currentElement.right)
                rightSet.updateAllIndexes()

                leftSet.addAll(rightSet)
                leftSet
            }
            else -> {
                throw IllegalStateException("Should have handled this type: $currentElement")
            }
        }
    }

     */

    // --------------------- Dictionary ----------------------

    /**
     * We transform an entire binary tree structure - that came form a [GtvDictionary] - back to an [GtvVirtualDictionary].
     *
     * [ProofNode] are meaningless, because didn't exist in the original array.
     * [ProofHashedLeaf] are also of no interest, since we don't know what the original value was.
     *
     * @param dictHeadElement the proof node that corresponds to the head of the original [GtvDictionary]
     */
    fun buildGtvVirtualDictionary(dictHeadElement: ProofNodeGtvDictHead): GtvVirtualDictionary {
        val tmpMap = buildGtvVirtualDictionaryInner(dictHeadElement)
        return GtvVirtualDictionary(tmpMap)
    }

    private fun buildGtvVirtualDictionaryInner(currentElement: MerkleProofElement): MutableMap<String, Gtv> {
        return when(currentElement) {
            is ProofHashedLeaf       -> mutableMapOf()                                     // Empty leaf
            is ProofValueGtvLeaf     -> {                                                  // Valuable leaf (at a leaf that holds a real value [Gtv])
                val key = (currentElement.pathElem as DictGtvPathElement).key
                val pair = Pair(key, currentElement.content)
                mutableMapOf(pair)
            }
            is ProofNodeGtvDictHead  -> {                                                  // Valuable leaf (at the the beginning of a new dict-tree)
                val key = (currentElement.pathElem as DictGtvPathElement).key
                val pair = Pair(key, buildGtvVirtualDictionary(currentElement))
                mutableMapOf(pair)
            }
            is ProofNodeGtvArrayHead -> {                                                  // Valuable leaf (at the the beginning of a new array-tree)
                val key = (currentElement.pathElem as DictGtvPathElement).key
                val pair = Pair(key, buildGtvVirtualArray(currentElement))
                mutableMapOf(pair)
            }
            is ProofNode -> {                                                              // Meaningless intermediary, should be removed.
                val leftSet = buildGtvVirtualDictionaryInner(currentElement.left)
                val rightSet = buildGtvVirtualDictionaryInner(currentElement.right)
                leftSet.putAll(rightSet)
                leftSet
            }
            else -> {
                throw IllegalStateException("Should have handled this type: ${currentElement::class.simpleName}")
            }
        }
    }

}