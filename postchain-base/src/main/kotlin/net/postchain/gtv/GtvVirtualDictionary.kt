// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv

import net.postchain.base.merkle.proof.MerkleProofElement
import net.postchain.core.UserMistake
import net.postchain.gtv.messages.RawGtv

/**
 * The virtual version of [GtvDictionary] only implements few of the methods defined in [Gtv].
 *
 * Note: If the user tries to use the virtual object like a real [Gtv] it will explode (Exception).
 * This is intentional, b/c using a virtual object for something other than data access could cause bugs.
 *
 * @property proofElement is cached here (see super class for desc)
 * @property dict is where we store sub-elements. This map will be mostly empty, and if the user asks for a key that
 *           doesn't exist we will explode (because we have no way of knowing if this key exists in the original dict).
 * @property size is the number of elements in the original dictionary (sometimes we don't know this).
 */
data class GtvVirtualDictionary(val proofElement: MerkleProofElement ,val dict: Map<String, Gtv>, val size: Int? = null) : GtvVirtual(proofElement) {

    override val type = GtvType.DICT // The virtual Dict pretends to be a normal [GtvDictionary].

    override operator fun get(key: String): Gtv? {
        val found = dict[key]
        if (found != null) {
            return found
        } else {
            throw UserMistake("The virtual dictionary doesn't keep the value for key = $key")
        }
    }

    /**
     * TODO: This is tricky! Should we use the original size or the current size?
     * The original size might cause bugs (if we try to extract all elements)
     */
    override fun getSize(): Int {
        return if (size != null) {
            size
        } else {
            dict.keys.size
        }
    }

    fun isKeyPresent(key: String): Boolean = dict[key] != null

    // ----------- These methods will explode -----------

    override fun asDict(): Map<String, Gtv> {
        throw UserMistake("Don't call this method on a virtual object")
    }

    override fun getRawGtv(): RawGtv {
        throw UserMistake("Don't call this method on a virtual object")
    }

    override fun asPrimitive(): Any? {
        throw UserMistake("Don't call this method on a virtual object")
    }

    override fun nrOfBytes(): Int {
        throw UserMistake("Don't call this method on a virtual object")
    }

    override fun equals(other: Any?): Boolean {
        throw UserMistake("You cannot compare a virtual object with something else.")
    }

    override fun hashCode(): Int {
        throw UserMistake("Don't call this method on a virtual object")
    }
}