package net.postchain.gtv

import net.postchain.base.merkle.proof.MerkleProofElement
import net.postchain.core.UserMistake
import net.postchain.gtv.messages.RawGtv

/**
 * The virtual version of [GtvArray] only implements few of the methods defined in [Gtv].
 *
 * Note: If the user tries to use the virtual object like a real [Gtv] it will explode (Exception).
 * This is intentional, b/c using a virtual object for something other than data access could cause bugs.
 *
 * @property proofElement is cached here (see super class for desc)
 * @property array is where we store sub elements. This wil be mostly "null", and if the user asks for a "null"
 *           we will explode (since we don't know what this value would be in the original [GtvArray] ).
 */
data class GtvVirtualArray(val proofElement: MerkleProofElement, val array: Array<out Gtv?>) : GtvVirtual(proofElement) {

    override val type = GtvType.ARRAY // The virtual Array pretends to be a normal [GtvArray].

    override operator fun get(index: Int): Gtv {
        val found = array[index]
        if (found != null) {
            return found
        } else {
            throw UserMistake("The virtual array doesn't keep the value at position $index")
        }
    }

    override fun getSize(): Int {
        return array.size
    }

    fun isKeyPresent(index: Int): Boolean = array[index] != null

    // ----------- These methods will explode -----------

    override fun asArray(): Array<out Gtv> {
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