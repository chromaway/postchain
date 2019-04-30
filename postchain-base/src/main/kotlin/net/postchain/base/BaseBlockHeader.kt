// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base

import net.postchain.base.gtv.BlockHeaderData
import net.postchain.base.gtv.BlockHeaderDataFactory
import net.postchain.common.toHex
import net.postchain.core.BlockHeader
import net.postchain.core.ByteArrayKey
import net.postchain.core.InitialBlockData
import net.postchain.core.UserMistake
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.generateProof
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkle.proof.GtvMerkleProofTree

/**
 * BaseBlockHeader implements elements and functionality that are necessary to describe and operate on a block header
 *
 * @property rawData DER encoded data including the previous blocks RID ([prevBlockRID]) and [timestamp]
 * @property cryptoSystem An implementation of the various cryptographic primitives to use
 * @property timestamp  Specifies the time that a block was created as the number
 *                      of milliseconds since midnight Januray 1st 1970 UTC
 */
class BaseBlockHeader(override val rawData: ByteArray, private val cryptoSystem: CryptoSystem) : BlockHeader {
    override val prevBlockRID: ByteArray
    override val blockRID: ByteArray
    val timestamp: Long get() = blockHeaderRec.getTimestamp()
    val blockHeaderRec: BlockHeaderData

    init {
        blockHeaderRec = BlockHeaderDataFactory.buildFromBinary(rawData)
        prevBlockRID = blockHeaderRec.getPreviousBlockRid()
        blockRID = cryptoSystem.digest(rawData)
    }

    companion object Factory {
        /**
         * Utility to simplify creating an instance of BaseBlockHeader
         *
         * @param cryptoSystem Cryptographic utilities
         * @param iBlockData Initial block data including previous block identifier, timestamp and height
         * @param rootHash Merkle tree root hash
         * @param timestamp timestamp
         * @return Serialized block header
         */
        @JvmStatic fun make(cryptoSystem: CryptoSystem, iBlockData: InitialBlockData, rootHash: ByteArray, timestamp: Long): BaseBlockHeader {
            val gtvBhd = BlockHeaderDataFactory.buildFromDomainObjects(iBlockData, rootHash, timestamp)

            val raw = GtvEncoder.encodeGtv(gtvBhd.toGtv())
            return BaseBlockHeader(raw, cryptoSystem)
        }
    }

    /**
     * Return a Merkle proof tree of a hash in a Merkle tree
     *
     * @param txHash Target hash for which the Merkle path is wanted
     * @param txHashes All hashes are the leaves part of this Merkle tree
     * @return The Merkle proof tree for [txHash]
     */
    fun merklePath(txHash: ByteArrayKey, txHashes: Array<ByteArrayKey>): GtvMerkleProofTree {
        //println("looking for tx hash: ${txHash.toHex()} in array where first is: ${txHashes[0].toHex()}")
        val positionOfOurTxToProve = txHashes.indexOf(txHash) //txHash.positionInArray(txHashes)
        if (positionOfOurTxToProve < 0) {
            throw UserMistake("We cannot prove this transaction (hash: ${txHash.byteArray.toHex()}), because it is not in the block")
        }
        val gtvArray = gtv(txHashes.map { gtv(it.byteArray)})
        val calculator = GtvMerkleHashCalculator(cryptoSystem)
        return gtvArray.generateProof(listOf(positionOfOurTxToProve), calculator)
    }

    /**
     * Validate that a Merkle path connects the target hash to the root hash in the block header
     *
     * @param merklePath The Merkle path
     * @param targetTxHash Target hash to validate path for
     * @return Boolean for if hash is part of the Merkle path
     */
    fun validateMerklePath(merklePath: MerklePath, targetTxHash: ByteArray): Boolean {
        return validateMerklePath(cryptoSystem, merklePath, targetTxHash, blockHeaderRec.getMerkleRootHash())
    }


}



