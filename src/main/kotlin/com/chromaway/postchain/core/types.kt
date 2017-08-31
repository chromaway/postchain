package com.chromaway.postchain.core

import java.sql.Connection;
import java.util.*

/*
1. Manager reads JSON and finds BlockchainConfigurationFactory class name.
2. Manager instantiates a class which implements BlockchainConfigurationFactory interface, and feeds it JSON data.
3. BlockchainConfigurationFactory creates BlockchainConfiguration object.
4. BlockchainConfiguration acts as a block factory and creates a transaction factory, presumably passing it configuration data in some form.
5. TransactionFactory will create Transaction objects according to configuration, possibly also passing it the configuration data.
6. Transaction object can perform its duties according to the configuration it received, perhaps creating sub-objects called transactors and passing them the configuration.
 */
// TODO: can we generalize conn? We can make it an Object, but then we have to do typecast everywhere...
open class EContext(val conn: Connection, val chainID: Int)

class BlockEContext(conn: Connection, chainID: Int, val blockIID: Long)
    : EContext(conn, chainID)

interface BlockHeader {
    val prevBlockRID: ByteArray;
    val rawData: ByteArray;
    val blockRID: ByteArray; // it's not a part of header but derived from it
}

open class BlockData(val header: BlockHeader, val transactions: Array<ByteArray>)

// Witness is a generalization over signatures
// Block-level witness is something which proves that block is valid and properly authorized

interface BlockWitness {
    val blockRID: ByteArray
    fun getRawData(): ByteArray
}

interface WitnessPart {

}

open class BlockDataWithWitness(header: BlockHeader, transactions: Array<ByteArray>, val witness: BlockWitness?)
    : BlockData(header, transactions)

// id is something which identifies subject which produces the
// signature, e.g. pubkey or hash of pubkey
class Signature(val subjectID: ByteArray, val data: ByteArray)

interface MultiSigBlockWitness : BlockWitness {
    fun getSignatures(): Array<Signature>;
}

interface BlockWitnessBuilder {
    fun isComplete(): Boolean
    fun getWitness(): BlockWitness // throws when not complete
}

interface MultiSigBlockWitnessBuilder : BlockWitnessBuilder {
    fun getMySignature(): Signature;
    fun applySignature(s: Signature);
}

// Transactor is an individual operation which can be applied to the database
// Transaction might consist of one or more operations
// Transaction should be serializable, but transactor doesn't need to have a serialized
// representation as we only care about storing of the whole Transaction

interface Transactor {
    fun isCorrect(): Boolean
    fun apply(ctx: BlockEContext): Boolean
}

interface Transaction : Transactor {
    fun getRawData(): ByteArray
}

// BlockchainConfiguration is a stateless objects which describes
// an individual blockchain instance within Postchain system

interface BlockchainConfiguration {
    val chainID: Long
    val traits: Set<String>

    fun decodeBlockHeader(rawBlockHeader: ByteArray): BlockHeader
    fun decodeWitness(rawWitness: ByteArray): BlockWitness
    fun getTransactionFactory(): TransactionFactory
    fun makeBlockBuilder(ctx: EContext): BlockBuilder
}

interface BlockchainConfigurationFactory {
    fun makeBlockchainConfiguration(chainID: Long, properties: Properties): BlockchainConfiguration
}

interface TransactionFactory {
    fun decodeTransaction(data: ByteArray): Transaction
}

interface BlockBuilder {
    fun begin()
    fun appendTransaction(tx: Transaction)
    fun appendTransaction(txData: ByteArray)
    fun finalize()
    fun finalizeAndValidate(bh: BlockHeader)
    fun getBlockData(): BlockData
    fun commit(w: BlockWitness?)
}

class InitialBlockData(val blockIID: Long, val prevBlockRID: ByteArray, val height: Long)

interface BlockStore {
    fun beginBlock(ctx: EContext): InitialBlockData
    fun finalizeBlock(bctx: BlockEContext, bh: BlockHeader)
    fun commitBlock(bctx: BlockEContext, w: BlockWitness?)
    fun getBlockHeight(ctx: EContext, blockRID: ByteArray): Long? // returns null if not found
    fun getBlockRID(ctx: EContext, height: Long): ByteArray? // returns null if height is out of range
    fun getLastBlockHeight(ctx: EContext): Long // height of the last block, first block has height 0
    fun getBlockData(ctx: EContext, height: Long): BlockData
    fun getWitnessData(ctx: EContext, height: Long): ByteArray
}