package net.postchain.base.merkle

import net.postchain.gtx.GTXValue


/**
 * Abstract class responsible for calculating hashes and serialization
 *
 * -----------
 * Motivation for the use of prefixes can be found at various places, for example:
 *
 * https://bitslog.wordpress.com/2018/06/09/leaf-node-weakness-in-bitcoin-merkle-tree-design/
 *
 * --------
 * (Excerpt from blog post)
 * The Problem
 *
 * Bitcoin Merkle tree makes no distinction between inner nodes and leaf nodes. The depth of the
 * tree is implicitly given by the number of transactions. Inner nodes have no specific format,
 * and are 64 bytes in length. Therefore, an attacker can submit to the blockchain a transaction that has
 * exactly 64 bytes in length and then force a victim system to re-interpret this transaction as an inner node
 * of the tree.
 *  An attacker can therefore provide an SPV proof (a Merkle branch) that adds an additional leaf node extending
 *  the dual transaction/node and provide proof of payment of any transaction he wishes.
 *
 * Remedy
 * (The fifth solution suggestioned is the one we are using:)
 * 5. A hard-forking solution is adding a prefix to internal nodes of the Merkle tree before performing node
 * hashing and adding a different prefix to transaction IDs and then also hash them.
 * Ripple ([2]) uses a such a prefix system.
 * --------
 *
 */
abstract class MerkleHashCalculator {

    val internalNodePrefix = byteArrayOf(0)
    val leafPrefix = byteArrayOf(1)

    /**
     * Leaf hashes are prefixed to tell them apart from internal nodes.
     *
     * @param gtxValue The leaf
     * @return the hash of a leaf.
     */
    abstract fun calculateLeafHash(gtxValue: GTXValue): Hash

    /**
     * Internal nodes' hashes are prefixed to tell them apart from leafs.
     *
     * @param hashLeft The hash of the left sub tree
     * @param hashRight The hash of the right sub tree
     * @return Returns the hash of two combined hashes.
     */
    abstract fun calculateNodeHash(hashLeft: Hash, hashRight: Hash): Hash

    /**
     * @param gtxValue The leaf
     * @param serializeFun The only reason we pass the function as a parameter is to simplify testing.
     * @param hashFun The only reason we pass the function as a parameter is to simplify testing.
     * @return the hash of the [GTXValue].
     */
    fun calculateHashOfGtxInternal(gtxValue: GTXValue, serializeFun: (GTXValue) -> ByteArray, hashFun: (ByteArray) -> Hash): Hash {
        var byteArr: ByteArray = serializeFun(gtxValue)
        val resultArr = leafPrefix + hashFun(byteArr)
        println("result: " + convertToHex(resultArr))
        return resultArr
    }

    /**
     * @param hashLeft The hash of the left sub tree
     * @param hashRight The hash of the right sub tree
     * @param hashFun The only reason we pass the function as a parameter is to simplify testing.
     * @return the hash of two combined hashes.
     */
    fun calculateNodeHashInternal(hashLeft: Hash, hashRight: Hash, hashFun: (ByteArray) -> Hash): Hash {
        val byteArraySum = hashLeft + hashRight
        return internalNodePrefix + hashFun(byteArraySum) // Adding the prefx at the last step
    }

    /**
     * Just used for testing
     *
     * @return A readable HEX string of the ByteArray
     */
    fun convertToHex(bytes: ByteArray): String {
        val sb: StringBuilder = StringBuilder()
        for (b in bytes) {
            val st = String.format("%02X", b)
            sb.append(st)
        }
        return sb.toString()
    }
}