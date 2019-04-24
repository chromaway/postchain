// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base.data

import mu.KLogging
import net.postchain.base.*
import net.postchain.common.toHex
import net.postchain.core.*
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash
import java.util.*

/**
 * BaseBlockBuilder is used to aid in building blocks, including construction and validation of block header and witness
 *
 * @property cryptoSystem Crypto utilities
 * @property eContext Connection context including blockchain and node identifiers
 * @property store For database access
 * @property txFactory Used for serializing transaction data
 * @property subjects Public keys for nodes authorized to sign blocks
 * @property blockSigMaker used to produce signatures on blocks for local node
 */
open class BaseBlockBuilder(
        val cryptoSystem: CryptoSystem,
        eContext: EContext,
        store: BlockStore,
        txFactory: TransactionFactory,
        val subjects: Array<ByteArray>,
        val blockSigMaker: SigMaker)
    : AbstractBlockBuilder(eContext, store, txFactory) {

    companion object : KLogging()

    private val calc = GtvMerkleHashCalculator(cryptoSystem)

    /**
     * Computes the root hash for the Merkle tree of transactions currently in a block
     *
     * @return The Merkle tree root hash
     */
    fun computeRootHash(): ByteArray {
        val digests = rawTransactions.map { txFactory.decodeTransaction(it).getHash() }

        val gtvArr = gtv(digests.map {gtv(it)})

        return gtvArr.merkleHash(calc)
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

        val rootHash = computeRootHash()
        if (logger.isDebugEnabled) {
            logger.debug("Create Block header. Root hash: ${rootHash.toHex()}, "+
                    " prev block: ${initialBlockData.prevBlockRID.toHex()} ," +
                    " height = ${initialBlockData.height} ")
        }
        val bh = BaseBlockHeader.make(cryptoSystem, initialBlockData, rootHash , timestamp)
        if (logger.isDebugEnabled) {
            logger.debug("Block header created with block RID: ${bh.blockRID.toHex()}.")
        }
        return bh
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

        val computedMerkleRoot = computeRootHash()
        // TODO: Remove these "debug" lines 2019-06-01 (nice to keep for now since we'll see what tests are not updated)
        println("computed MR: ${computedMerkleRoot.toHex()}")
        println("header MR: ${header.blockHeaderRec.getMerkleRootHash().toHex()}")
        return when {
            !Arrays.equals(header.prevBlockRID, initialBlockData.prevBlockRID) ->
                ValidationResult(false, "header.prevBlockRID != initialBlockData.prevBlockRID," +
                        "( ${header.prevBlockRID.toHex()} != ${initialBlockData.prevBlockRID.toHex()} ), "+
                        " height: ${header.blockHeaderRec.height} and ${initialBlockData.height} ")

            header.blockHeaderRec.getHeight() != initialBlockData.height ->
                ValidationResult(false, "header.blockHeaderRec.height != initialBlockData.height")

            bctx.timestamp >= header.timestamp ->
                ValidationResult(false, "bctx.timestamp >= header.timestamp")

            !Arrays.equals(header.blockHeaderRec.getMerkleRootHash(), computedMerkleRoot) -> // Do this last since most expensive check!
                ValidationResult(false, "header.blockHeaderRec.rootHash != computeRootHash()")

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
        witnessBuilder.applySignature(blockSigMaker.signDigest(_blockData!!.header.blockRID)) // TODO: POS-04_sig
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