package net.postchain.devtools.utils.configuration

import net.postchain.base.BlockchainRelatedInfo
import net.postchain.common.toHex
import net.postchain.devtools.KeyPairHelper.pubKeyFromByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.gtvml.GtvMLParser


/**
 * This setup holds the most important facts about a blockchain we will use during the test.
 *
 * Note: the reason we have 2 sets for BC dependencies is bc we initially only know the BC RID (from the config file).
 *       The chainID has to be added later. (This is bit ugly but I have no good idea how to clean it up in a better way)
 *
 * @property chainId is the ID of the chain
 * @property rid is the blockchainRID
 * @property bcGtv is the BC configuration in the form of [GTV] dictionary
 * @property signerNodeList is a list of [NodeSeqNumber] that must sign this chain
 * @property chainDependencies is a set of the other chain IDs this chain depends on
 */
data class BlockchainSetup(
        val chainId: Int,
        val rid: String,
        val bcGtv: Gtv,
        val signerNodeList: List<NodeSeqNumber>,
        val chainDependencies: Set<Int> = setOf() // default is none
) {

    companion object {

        /**
         * Will:
         * 1. figure out the RID via the cache
         */
        fun simpleBuild(chainId: Int, filepath: String, bcGtv: Gtv, signers: List<NodeSeqNumber>): BlockchainSetup {
            return BlockchainSetup(chainId, TestBlockchainRidCache.getRid(chainId), bcGtv, signers)
        }


        /**
         * Will:
         * 1. figure out the RID via the cache
         * 2. figure out the dependency chain ID via the cache
         */
        fun buildWithDependencies(chainId: Int, bcGtv: Gtv, signers: List<NodeSeqNumber>, dependencyRid: Set<String>): BlockchainSetup {

            val depChainIds = dependencyRid.map { TestBlockchainRidCache.getChainId(it)}.toSet()
            return BlockchainSetup(chainId, TestBlockchainRidCache.getRid(chainId), bcGtv, signers, depChainIds)
        }

    }

    fun isNodeSigner(nodeNr: NodeSeqNumber) = signerNodeList.find { it == nodeNr } != null

}