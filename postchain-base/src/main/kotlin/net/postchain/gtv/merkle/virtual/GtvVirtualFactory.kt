package net.postchain.gtv.merkle.virtual

import mu.KLogging
import net.postchain.base.merkle.proof.*
import net.postchain.core.UserMistake
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvVirtual
import net.postchain.gtv.GtvVirtualArray
import net.postchain.gtv.GtvVirtualDictionary
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree
import net.postchain.gtv.merkle.proof.ProofNodeGtvArrayHead
import net.postchain.gtv.merkle.proof.ProofNodeGtvDictHead
import net.postchain.gtv.merkle.proof.ProofValueGtvLeaf
import net.postchain.gtv.path.ArrayGtvPathElement
import net.postchain.gtv.path.DictGtvPathElement
import net.postchain.gtv.path.SearchableGtvPathElement


/**
 * This factory can build [GtvVirtual] structures out of proof trees
 */
object GtvVirtualFactory: KLogging() {

    /**
     * Turns a proof tree into a virtual GTV.
     *
     * Note: There is this strange corner case where the proof in the ENTIRE structure (i.e. the proof tree and the
     * original source GTV object graph are the same). I have chosen not to handle trivial cases, the
     * result would be null anyway.
     *
     * @return a [GtvVirtual] that corresponds to the original [Gtv]
     */
    fun buildGtvVirtual(proofTree: GtvMerkleProofTree): GtvVirtual {
        if (logger.isDebugEnabled) {
            logger.debug("--------------------------------------------")
            logger.debug("--- Converting proof tree to virtual GTV ---")
            logger.debug("--------------------------------------------")
        }
        val res = buildGtvVirtualInner(proofTree.root)
        if (logger.isDebugEnabled) {
            logger.debug("Virtual GTV Built: $res")
            logger.debug("--------------------------------------------")
            logger.debug("--- /Converting proof tree to virtual GTV --")
            logger.debug("--------------------------------------------")
        }
        return res
    }

    private fun buildGtvVirtualInner(currentElement: MerkleProofElement): GtvVirtual {
        return when (currentElement) {
            is ProofNodeGtvArrayHead -> buildGtvVirtualArray(currentElement, true)
            is ProofNodeGtvDictHead  -> buildGtvVirtualDictionary(currentElement, true)
            is ProofNode             -> throw UserMistake("A proof structure cannot have an (internal) node as root.")
            is ProofHashedLeaf       -> throw UserMistake("A proof structure cannot have a hash as root.")
            is ProofValueLeaf<*>     -> throw UserMistake("A proof structure cannot be just the value that should be proven (meaningless).") // TODO: Maybe this shoud change?
            else                     -> throw UserMistake("We don't handle proofs that begin with type: ${currentElement::class.simpleName}")
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
     * @param isRoot is true if this is the top element (the top element does not have a path elem)
     */
    fun buildGtvVirtualArray(arrHeadElement: ProofNodeGtvArrayHead, isRoot: Boolean = false): GtvVirtualArray {
        return if (isRoot) {
            val tmpSet = handleArrLeftAndRight(arrHeadElement.left, arrHeadElement.right)
            if (logger.isDebugEnabled) { logger.debug("Building root array (found ${tmpSet.innerSet.size} elements).") }
            tmpSet.buildGtvVirtualArray(arrHeadElement, arrHeadElement.size)
        } else {
            val tmpSet = buildGtvVirtualArrayInner(arrHeadElement)
            if (logger.isDebugEnabled) { logger.debug("Building array (found ${tmpSet.innerSet.size} elements).") }
            tmpSet.buildGtvVirtualArray(arrHeadElement, arrHeadElement.size)
        }
    }

    private fun handleArrLeftAndRight(left: MerkleProofElement, right: MerkleProofElement): ArrayIndexAndGtvList {
        val left = buildGtvVirtualArrayInner(left)
        val right = buildGtvVirtualArrayInner(right)
        left.addAll(right)
        return left
    }

    private fun getIndex(pathElem: SearchableGtvPathElement) =  (pathElem as ArrayGtvPathElement).index


    private fun buildGtvVirtualArrayInner(currentElement: MerkleProofElement): ArrayIndexAndGtvList {
        return when(currentElement) {
            is ProofHashedLeaf       -> ArrayIndexAndGtvList()                 // Empty leaf (we don't care about these)
            is ProofValueGtvLeaf     -> {                                     // Valuable leaf (at a leaf that holds a real value [Gtv])
                val index = getIndex(currentElement.pathElem)
                ArrayIndexAndGtvList(index, currentElement.content)
            }
            is ProofNodeGtvDictHead  -> {                                     // Valuable leaf (at the the beginning of a new dict-tree)
                val index = getIndex(currentElement.pathElem!!)
                val tmpMap = handleLDictLeftAndRight(currentElement.left, currentElement.right)
                val dict = GtvVirtualDictionary(currentElement, tmpMap, currentElement.size)
                if (logger.isDebugEnabled) { logger.debug("Put virtual dict (size: ${dict.getSize()} ) in array at pos $index.") }
                ArrayIndexAndGtvList(index, dict)
            }
            is ProofNodeGtvArrayHead -> {                                     // Valuable leaf (at the the beginning of a new array-tree)
                val index = getIndex(currentElement.pathElem!!)
                val tmpSet = handleArrLeftAndRight(currentElement.left, currentElement.right)
                val arr = tmpSet.buildGtvVirtualArray(currentElement, currentElement.size)
                if (logger.isDebugEnabled) { logger.debug("Put virtual array (size: ${arr.getSize()} ) in array at pos $index..") }
                ArrayIndexAndGtvList(index, arr)
            }
            is ProofNode -> {                                                 // Meaningless intermediary (does not represent anything in the GtvVirtual)
                handleArrLeftAndRight(currentElement.left, currentElement.right)
            }
            else -> {
                throw IllegalStateException("Should have handled this type: $currentElement")
            }
        }
    }

    // --------------------- Dictionary ----------------------

    /**
     * We transform an entire binary tree structure - that came form a [GtvDictionary] - back to an [GtvVirtualDictionary].
     *
     * [ProofNode] are meaningless, because didn't exist in the original array.
     * [ProofHashedLeaf] are also of no interest, since we don't know what the original value was.
     *
     * @param dictHeadElement the proof node that corresponds to the head of the original [GtvDictionary]
     * @param isRoot is true if this is the top element (the top element does not have a path elem)
     */
    fun buildGtvVirtualDictionary(dictHeadElement: ProofNodeGtvDictHead, isRoot: Boolean = false): GtvVirtualDictionary {
        return if (isRoot) {
            val tmpMap = handleLDictLeftAndRight(dictHeadElement.left, dictHeadElement.right)
            if (logger.isDebugEnabled) { logger.debug("Building root dict (found ${tmpMap.keys.size} elements).") }
            GtvVirtualDictionary(dictHeadElement, tmpMap, dictHeadElement.size)
        } else {
            val tmpMap = buildGtvVirtualDictionaryInner(dictHeadElement)
            if (logger.isDebugEnabled) { logger.debug("Building dict (found ${tmpMap.keys.size} elements).") }
            GtvVirtualDictionary(dictHeadElement, tmpMap, dictHeadElement.size)
        }
    }

    private fun handleLDictLeftAndRight(left: MerkleProofElement, right: MerkleProofElement): MutableMap<String, Gtv> {
        val leftMap = buildGtvVirtualDictionaryInner(left)
        val rightMap = buildGtvVirtualDictionaryInner(right)
        leftMap.putAll(rightMap)
        return leftMap
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
                val dictMap = handleLDictLeftAndRight(currentElement.left, currentElement.right)
                val dict = GtvVirtualDictionary(currentElement, dictMap, currentElement.size)
                if (logger.isDebugEnabled) { logger.debug("Put virtual dict (size: ${dict.getSize()} ) in array at key: $key.") }
                mutableMapOf(Pair(key, dict))
            }
            is ProofNodeGtvArrayHead -> {                                                  // Valuable leaf (at the the beginning of a new array-tree)
                val key = (currentElement.pathElem as DictGtvPathElement).key
                val tmpSet = handleArrLeftAndRight(currentElement.left, currentElement.right)
                val arr = tmpSet.buildGtvVirtualArray(currentElement, currentElement.size)
                if (logger.isDebugEnabled) { logger.debug("Put virtual array (size: ${arr.getSize()} ) in array at key: $key.") }
                mutableMapOf(Pair(key, arr))
            }
            is ProofNode -> {                                                              // Meaningless intermediary (does not represent anything in the GtvVirtual)
                handleLDictLeftAndRight(currentElement.left, currentElement.right)
            }
            else -> {
                throw IllegalStateException("Should have handled this type: ${currentElement::class.simpleName}")
            }
        }
    }

}