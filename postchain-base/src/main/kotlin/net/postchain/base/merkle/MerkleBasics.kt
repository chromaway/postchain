// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.merkle

import net.postchain.base.CryptoSystem
import net.postchain.common.data.Hash
import net.postchain.core.ProgrammerMistake
import net.postchain.gtv.Gtv
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree

/**
 * This file holds some basic stuff, like constants etc..
 *
 * Some generic documentation (package wide documentation) goes here too, see below:
 *
 * ---------
 * Design Overview
 * ---------
 * The way to create a merkle proof tree and to actually prove it using the code in this package is done this way:
 *
 * 1. Transform the hierarchy (usually a [Gtv]) to a binary tree (usually a [GtvBinaryTree]).
 *       - The reason we have this unnecessary step, is to be able to look at the binary tree in tests before it's
 *       destroyed (Easier to find bugs if we move in small steps).
 * 2. Transform the binary tree into a proof tree (usually a [GtvMerkleProofTree]).
 *       - To do this we have to provide a path (usually [GTXPath]) to the element(s) we want to prove.
 *       - The proof tree will hold the element(s) (=values-to-be-proven) in clear text, but the rest of the tree is
 *     only hashes.
 *       - The proof tree is the "proof" that the element(s) belongs in the hierarchy.
 * 3. Calculate the merkle root of the proof tree.
 *       - If the merkle root is the same as the merkle root described in the block, we know the proof is valid.
 * ---------
 *
 * ---------
 * Prefixes added before we hash
 * ---------
 * Motivation for the use of prefixes can be found at various places, for example:
 *
 * https://bitslog.wordpress.com/2018/06/09/leaf-node-weakness-in-bitcoin-merkle-tree-design/
 *
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
 */


object MerkleBasics {

    /**
     * Prefixes are put in front of the the hash, after the hash value has been calculated
     */
    const val HASH_PREFIX_NODE: Byte = 0
    const val HASH_PREFIX_LEAF: Byte = 1

    /**
     * Use this to represent a hash of an empty element (in a tree, typically)
     */
    val EMPTY_HASH = ByteArray(32) // Just zeros

    const val UNKNOWN_SIZE_IN_BYTE = -101

    /**
     * This should be the hashing function we use in production
     *
     * @param bArr is the data to hash
     * @param cryptoSystem used to get the hash function
     * @return the hash we calculated
     */
    fun hashingFun(bArr: ByteArray, cryptoSystem: CryptoSystem?): Hash {
        if (cryptoSystem == null) {
            throw ProgrammerMistake("In this case we need the CryptoSystem to calculate the hash")
        }  else {
            return cryptoSystem.digest(bArr)
        }
    }
}