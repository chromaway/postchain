// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base.data

import net.postchain.base.*
import net.postchain.core.*
import java.util.*

/**
 * BaseBlockBuilder is used to aid in building blocks, including construction and validation of block header and witness
 *
 * @property cryptoSystem Crypto utilities
 * @property eContext Connection context including blockchain and node identifiers
 * @property store For database access
 * @property txFactory Used for serializing transaction data
 * @property subjects Public keys for nodes authorized to sign blocks
 * @property blockSigner Signing function for local node to sign block
 */
open class BaseBlockBuilder(val cryptoSystem: CryptoSystem, eContext: EContext, store: BlockStore,
                            txFactory: TransactionFactory, val subjects: Array<ByteArray>, val blockSigner: Signer)
    : AbstractBlockBuilder(eContext, store, txFactory) {


    /**
     * Computes the root hash for the Merkle tree of transactions currently in a block
     *
     * @return The Merkle tree root hash
     */
    fun computeRootHash(): ByteArray {
        val digests = rawTransactions.map { txFactory.decodeTransaction(it).getHash() }
        return computeMerkleRootHash(cryptoSystem, digests.toTypedArray())
    }

    /**
     * Create block header from initial block data
     *
     * @return Block header
     */
    override fun makeBlockHeader(): BlockHeader {
        var timestamp = System.currentTimeMillis()
        if (timestamp <= initialBlockData.timestamp) {
            // if our time is behind the timestamp of most recent block, do a minimal increment
            timestamp = initialBlockData.timestamp + 1
        }

        return BaseBlockHeader.make(cryptoSystem, initialBlockData, computeRootHash(), timestamp)
    }

    /**
     * Validate block header:
     * - check that previous block RID is used in this block
     * - check for correct height
     * - check for correct root hash
     * - check that timestamp occurs after previous blocks timestamp
     *
     * @param blockHeader The block header to validate
     */
    override fun validateBlockHeader(blockHeader: BlockHeader): ValidationResult {
        val header = blockHeader as BaseBlockHeader

        return when {
            !Arrays.equals(header.prevBlockRID, initialBlockData.prevBlockRID) ->
                ValidationResult(false, "header.prevBlockRID != initialBlockData.prevBlockRID")

            header.blockHeaderRec.height != initialBlockData.height ->
                ValidationResult(false, "header.blockHeaderRec.height != initialBlockData.height")

            !Arrays.equals(header.blockHeaderRec.rootHash, computeRootHash()) ->
                ValidationResult(false, "header.blockHeaderRec.rootHash, computeRootHash()")

            bctx.timestamp >= header.timestamp ->
                ValidationResult(false, "bctx.timestamp >= header.timestamp")

            else -> ValidationResult(true)
        }
    }

    /**
     * Validates the following:
     *  - Witness is of a correct implementation
     *  - The signatures are valid with respect to the block being signed
     *  - The number of signatures exceeds the threshold necessary to deem the block itself valid
     *
     *  @param blockWitness The witness to be validated
     *  @throws ProgrammerMistake Invalid BlockWitness implementation
     */
    override fun validateWitness(blockWitness: BlockWitness): Boolean {
        if (!(blockWitness is MultiSigBlockWitness)) {
            throw ProgrammerMistake("Invalid BlockWitness impelmentation.")
        }
        val witnessBuilder = BaseBlockWitnessBuilder(cryptoSystem, _blockData!!.header, subjects, getRequiredSigCount())
        for (signature in blockWitness.getSignatures()) {
            witnessBuilder.applySignature(signature)
        }
        return witnessBuilder.isComplete()
    }

    /**
     * Retrieve the builder for block witnesses. It can only be retrieved if the block is finalized.
     *
     * @return The block witness builder
     * @throws ProgrammerMistake If the block is not finalized yet signatures can't be created since they would
     * be invalid when further transactions are added to the block
     */
    override fun getBlockWitnessBuilder(): BlockWitnessBuilder? {
        if (!finalized) {
            throw ProgrammerMistake("Block is not finalized yet.")
        }

        val witnessBuilder = BaseBlockWitnessBuilder(cryptoSystem, _blockData!!.header, subjects, getRequiredSigCount())
        witnessBuilder.applySignature(blockSigner(_blockData!!.header.rawData))
        return witnessBuilder
    }

    /**
     * Return the number of signature required for a finalized block to be deemed valid
     *
     * @return An integer representing the threshold value
     */
    protected open fun getRequiredSigCount(): Int {
        val requiredSigs: Int
        if (subjects.size == 1)
            requiredSigs = 1
        else if (subjects.size == 3) {
            requiredSigs = 3
        } else {
            val maxFailedNodes = Math.floor(((subjects.size - 1) / 3).toDouble())
            //return signers.signers.length - maxFailedNodes;
            requiredSigs = 2 * maxFailedNodes.toInt() + 1
        }
        return requiredSigs
    }

}