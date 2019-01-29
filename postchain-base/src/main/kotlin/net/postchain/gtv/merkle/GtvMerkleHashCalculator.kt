package net.postchain.gtv.merkle

import net.postchain.base.CryptoSystem
import net.postchain.base.merkle.*
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree
import net.postchain.gtv.merkle.proof.GtvMerkleProofTreeFactory
import net.postchain.core.ProgrammerMistake

import net.postchain.gtv.*
import net.postchain.gtv.GtvEncoder.encodeGtv

/**
 * This should be the serialization we use in production
 *
 * @param Gtv to serialize
 * @return the byte args containing serialized data
 */
fun serializeGtvToByteArary(Gtv: Gtv): ByteArray {
    return when (Gtv) {
        is GtvNull ->      encodeGtv (Gtv)
        is GtvInteger ->   encodeGtv(Gtv)
        is GtvString ->    encodeGtv(Gtv)
        is GtvByteArray -> encodeGtv(Gtv)
        is GtvArray ->     throw ProgrammerMistake("Gtv is an args (We should have transformed all collection-types to trees by now)")
        is GtvDictionary ->throw ProgrammerMistake("Gtv is an dict (We should have transformed all collection-types to trees by now)")
        else -> {
            // TODO: Log a warning here? We don't know what this is!
            encodeGtv(Gtv) // Hope for the best
        }
    }
}

/**
 * The calculator intended to be used is production for trees that hold [Gtv]
 */
class GtvMerkleHashCalculator(cryptoSystem: CryptoSystem): MerkleHashCalculator<Gtv, GtvPath>(cryptoSystem) {

    val treeFactory = GtvBinaryTreeFactory()

    var proofTreeFactory: GtvMerkleProofTreeFactory
    var baseCalc: BinaryNodeHashCalculator
    init {
        proofTreeFactory = GtvMerkleProofTreeFactory(this)
        baseCalc = BinaryNodeHashCalculatorBase(cryptoSystem)
    }

    override fun calculateNodeHash(prefix: Byte, hashLeft: Hash, hashRight: Hash): Hash {
        return baseCalc.calculateNodeHash(prefix, hashLeft, hashRight)
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
        return value.isContainerType()
    }

    override fun buildTreeFromContainerValue(value: Gtv): GtvMerkleProofTree {
        val root: GtvBinaryTree = treeFactory.buildFromGtv(value)
        return proofTreeFactory.buildFromBinaryTree(root)
    }

}