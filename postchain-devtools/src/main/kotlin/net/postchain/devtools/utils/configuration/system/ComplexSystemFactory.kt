package net.postchain.devtools.utils.configuration.system

import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.NodeSeqNumber
import net.postchain.devtools.utils.configuration.NodeSetup
import net.postchain.devtools.utils.configuration.SystemSetup

/**
 * This factory replaces the need for node configuration files in (most) tests. The nodes needed will be calculated.
 *
 * Note: if what you want to test is incorrect or strange node configuration, you should not use this factory
 * but use a real node config file.
 */

object ComplexSystemFactory {

    /**
     * Complex setup means we look at the blockchain configurations given, and figure out what nodes we need from that.
     * The [NodeSetup] we will create will know exactly what nodes they should connect to
     *
     * @param blockchainConfList holds the complete blockchain configurations
     */
    fun buildSystemSetup(
            blockchainConfList: List<BlockchainSetup>
    ): SystemSetup {
        val tmpNodeMap = mutableMapOf<NodeSeqNumber, NodeSetup>()
        val tmpBlockchainMap = mutableMapOf<Int, BlockchainSetup>()

        // 1. Get all nodes from the blockchain configs
        val nodeNrList = calculateAllNodesNeeded(blockchainConfList)

        // 2. Just add the BC confs
        for (chainConf in blockchainConfList) {
            tmpBlockchainMap[chainConf.chainId] = chainConf
        }
        val bcMap = tmpBlockchainMap.toMap()

        // 3. Create the nodes
        for (nodeNr in nodeNrList) {
            val bcsToSign = calculateWhatBlockchainsTheNodeShouldSign(nodeNr, bcMap)
            val bcsToRead = calculateWhatBlockchainsTheNodeShouldRead(bcsToSign, bcMap)
            tmpNodeMap[nodeNr] = NodeSetup.buildSimple(nodeNr, bcsToSign, bcsToRead)
        }

        return SystemSetup(
                tmpNodeMap.toMap(),
                bcMap)
    }

    /**
     * @return all nodes we need to have in the system from the signer lists of the [BlockchainSetup]
     */
    fun calculateAllNodesNeeded(blockchainConfList: List<BlockchainSetup>): List<NodeSeqNumber> {
        val retSet = mutableSetOf<NodeSeqNumber>()

        for (bc in blockchainConfList) {
            retSet.addAll(bc.signerNodeList) // Since it's a set all duplicates will be removed
        }

        return retSet.toList()
    }

    /**
     * We can figure out what chains to sign by just go through all chains and look in the corresponding signer list.
     *
     * @param nodeNr is the node we are talking about
     * @param blockchainMap is the dictionary of all chains in the system
     */
    fun calculateWhatBlockchainsTheNodeShouldSign(
            nodeNr: NodeSeqNumber,
            blockchainMap: Map<Int, BlockchainSetup>
    ): Set<Int> {
        val retSet = mutableSetOf<Int>()

        // Check the blockchain Setups is this node is a signer?
        for (bc in blockchainMap.values) {
            if (bc.isNodeSigner(nodeNr)) {
                retSet.add(bc.chainId)
            }
        }

        return retSet.toSet()
    }


    /**
     * Check the blockchain Setups is this node is a signer and the BC has dep.
     *
     * This function will call itself recursively
     *
     * @param startingBcs are the chains we should check for dependencies
     * @param blockchainMap is the dictionary for all chains in our system
     * @return a set of all chain IDs we need to read (this is a SET since we don't want duplicates)
     */
    fun calculateWhatBlockchainsTheNodeShouldRead(
            startingBcs: Set<Int>,
            blockchainMap: Map<Int, BlockchainSetup>
    ): Set<Int> {
        val retSet = mutableSetOf<Int>()

        for (bcId in startingBcs) {
            val bc = blockchainMap[bcId]!!
            if (bc.chainDependencies.isNotEmpty()) {
                // We should read all these BCs
                retSet.addAll(bc.chainDependencies)

                // Don't forget that each dependency might have a dependency
                val depConns = calculateWhatBlockchainsTheNodeShouldRead(bc.chainDependencies, blockchainMap)
                retSet.addAll(depConns)
            }
        }
        return retSet.toSet()
    }
}