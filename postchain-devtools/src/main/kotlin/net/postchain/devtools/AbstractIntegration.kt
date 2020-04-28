package net.postchain.devtools

import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.core.BlockBuilder
import net.postchain.core.BlockWitness
import net.postchain.core.BlockchainEngine
import net.postchain.core.MultiSigBlockWitnessBuilder
import net.postchain.gtv.Gtv
import net.postchain.gtv.gtvml.GtvMLParser


/**
 * Postchain has different test categories:
 *
 * 1. Unit tests - A test whithout dependencies.
 * 2. Integration tests - Depends on the DB.
 * 3. Nightly test - Tests with a lot of data.
 * 4. Manual test - Requires some manual work to run.
 *
 * Type 2-4 are often heavy, and should inherit this class to get help doing common tasks.
 * Examples of tasks this class will help you with are:
 *
 * - Creating a configuration for:
 *    - single node
 *    - multiple nodes
 * - Verifying all transactions in the BC
 * - Building and committing a block
 * - etc
 */
abstract class AbstractIntegration {

    val cryptoSystem = SECP256K1CryptoSystem()

    companion object {
        const val BASE_PORT = 9870
    }

    /**
     * Put logic in here that should run after each test (the "@after" annotation will guarantee execution)
     */
    abstract fun tearDown()

    protected fun readBlockchainConfig(blockchainConfigFilename: String): Gtv {
        return GtvMLParser.parseGtvML(
                javaClass.getResource(blockchainConfigFilename).readText())
    }

    protected fun buildBlockAndCommit(engine: BlockchainEngine) {
        val (blockBuilder, _) = engine.buildBlock()
        commitBlock(blockBuilder)
    }

    protected fun buildBlockAndCommit(node: PostchainTestNode) {
        commitBlock(node
                .getBlockchainInstance()
                .getEngine()
                .buildBlock()
                .first)
    }

    private fun commitBlock(blockBuilder: BlockBuilder): BlockWitness {
        val witnessBuilder = blockBuilder.getBlockWitnessBuilder() as MultiSigBlockWitnessBuilder
        val blockData = blockBuilder.getBlockData()
        // Simulate other peers sign the block
        val blockHeader = blockData.header
        var i = 0
        while (!witnessBuilder.isComplete()) {
            val sigMaker = cryptoSystem.buildSigMaker(KeyPairHelper.pubKey(i), KeyPairHelper.privKey(i))
            witnessBuilder.applySignature(sigMaker.signDigest(blockHeader.blockRID))
            i++
        }
        val witness = witnessBuilder.getWitness()
        blockBuilder.commit(witness)
        return witness
    }

    protected fun getTxRidsAtHeight(node: PostchainTestNode, height: Long): Array<ByteArray> {
        val blockQueries = node.getBlockchainInstance().getEngine().getBlockQueries()
        val blockRid = blockQueries.getBlockRid(height).get()
        return blockQueries.getBlockTransactionRids(blockRid!!).get().toTypedArray()
    }

    protected fun getBestHeight(node: PostchainTestNode): Long {
        return node.getBlockchainInstance().getEngine().getBlockQueries().getBestHeight().get()
    }
}