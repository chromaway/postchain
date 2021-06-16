// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtv.merkle

import net.postchain.base.CryptoSystem
import net.postchain.common.data.Hash
import net.postchain.base.merkle.MerkleBasics
import net.postchain.base.merkle.MerkleHashCalculator
import net.postchain.core.ProgrammerMistake
import net.postchain.gtv.*
import net.postchain.gtv.GtvEncoder.encodeGtv

/**
 * This should be the serialization we use in production
 *
 * @param gtv to serialize
 * @return the byte array containing serialized data
 */
fun serializeGtvToByteArary(gtv: Gtv): ByteArray {
    return when (gtv) {
        is GtvNull      -> encodeGtv(gtv)
        is GtvInteger   -> encodeGtv(gtv)
        is GtvString    -> encodeGtv(gtv)
        is GtvByteArray -> encodeGtv(gtv)
        is GtvPrimitive -> {
            // TODO: Log a warning here? We don't know what this is!
            encodeGtv(gtv) // Hope for the best, because all primitives should be able to do this.
        }
        is GtvCollection -> throw ProgrammerMistake("Gtv is a collection (We should have transformed all collection-types to trees by now)")
        else             -> throw ProgrammerMistake("Note a primitive and not a collection: what is it? type: ${gtv.type}")
    }
}

/**
 * The calculator intended to be used is production for trees that hold [Gtv]
 */
class GtvMerkleHashCalculator(cryptoSystem: CryptoSystem):
        MerkleHashCalculator<Gtv>(cryptoSystem) {

    override fun calculateNodeHash(prefix: Byte, hashLeft: Hash, hashRight: Hash): Hash {
        return calculateNodeHashInternal(prefix, hashLeft, hashRight, MerkleBasics::hashingFun)
    }

    /**
     * Leafs hashes are prefixed to tell them apart from internal nodes
     *
     * @param value The leaf
     * @return Returns the hash of the leaf.
     */
    override fun calculateLeafHash(value: Gtv): Hash {
        return calculateHashOfValueInternal(value, ::serializeGtvToByteArary, MerkleBasics::hashingFun)
    }

    override fun isContainerProofValueLeaf(value: Gtv): Boolean {
        return when (value) {
            is GtvCollection -> true
            is GtvPrimitive -> false
            else -> throw IllegalStateException("The type is neither collection or primitive. type: ${value.type} ")
        }
    }

}