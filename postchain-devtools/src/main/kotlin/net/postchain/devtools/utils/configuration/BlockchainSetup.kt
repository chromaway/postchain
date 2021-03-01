package net.postchain.devtools.utils.configuration

import net.postchain.base.BlockchainRid
import net.postchain.devtools.PostchainTestNode
import net.postchain.gtv.Gtv


/**
 * This setup holds the most important facts about a blockchain we will use during the test.
 * We have the entire BC configuration in the GTV format, but we have extracted the signers and dependencies for convenience.
 *
 * (The "setup" classes are data holders/builders for test configuration used to generate the "real" classes at a later stage)
 *
 * @property chainId is the ID of the chain
 * @property rid is the blockchainRID
 * @property bcGtv is the BC configuration in the form of [GTV] dictionary
 * @property signerNodeList is a list of [NodeSeqNumber] that must sign this chain
 * @property chainDependencies is a set of the other chain IDs this chain depends on
 */
data class BlockchainSetup(
        val chainId: Int,
        val rid: BlockchainRid,
        val bcGtv: Gtv,
        val signerNodeList: List<NodeSeqNumber>,
        val chainDependencies: Set<Int> = setOf() // default is none
) {

    /**
     * Function with default bc preparations. These commands are separated here, so that they can be replaced with another
     * prepare function. An example is found in AbstractSyncTest.kt
     */
    var prepareBlockchainOnNode: (BlockchainSetup, PostchainTestNode) -> Unit = { blockchainSetup, node ->
        node.addBlockchain(blockchainSetup)
        node.mapBlockchainRID(blockchainSetup.chainId.toLong(), blockchainSetup.rid)
    }

    companion object {

        /**
         * Will:
         * 1. figure out the RID via the GTV config
         * 2. figure out the dependency chain ID via the cache (we must have added the dependencies to the cache before this)
         */
        fun buildWithDependencies(
                chainId: Int,
                bcGtv: Gtv,
                signers: List<NodeSeqNumber>,
                dependencyRid: Set<BlockchainRid>
        ): BlockchainSetup {
            val depChainIds = dependencyRid.map {
                println("dep chainId: $it, adding to cache");
                TestBlockchainRidCache.getChainId(it)
            }.toSet()
            return BlockchainSetup(chainId, TestBlockchainRidCache.getRid(chainId, bcGtv), bcGtv, signers, depChainIds)
        }

        /**
         * Will take a GTV configuration and return a setup
         */
        fun buildFromGtv(chainId: Int, gtvConfig: Gtv): BlockchainSetup = BlockchainSetupFactory.buildFromGtv(chainId, gtvConfig)


    }

    fun isNodeSigner(nodeNr: NodeSeqNumber) = signerNodeList.find { it == nodeNr } != null

}