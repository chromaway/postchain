package net.postchain.devtools.utils.configuration.system

import mu.KLogging
import net.postchain.base.BlockchainRelatedInfo
import net.postchain.common.toHex
import net.postchain.devtools.KeyPairHelper
import net.postchain.devtools.utils.configuration.*
import net.postchain.devtools.utils.configuration.pre.BlockchainPreSetup
import net.postchain.devtools.utils.configuration.pre.SystemPreSetup
import java.lang.IllegalStateException

/**
 * This factory replaces the need for node configuration files in (most) tests. The nodes' conf will be calculated.
 * (Before you use this factory you must have created all [BlockchainSetup] needed for the test. Tip: use [SystemPreSetup] for this).
 *
 *  (The "setup" classes are data holders/builders for test configuration used to generate the "real" classes at a later stage)
 *
 * Note: if what you want to test is incorrect or strange node configuration, you should not use this factory
 * but use real node config files.
 */

object SystemSetupFactory : KLogging() {

    /**
     * Takes a [SystemPreSetup] and builds a [SystemSetup].
     *
     * Note: The reason why we do this in two steps (i.e. XPreSetup -> XSetup) is that we in many tests need to modify
     * the [SystemPreSetup] before we generate the [SystemSetup] from it.
     *
     * Note2: The implementation is trick for one reason: we MUST create the [BlockchainSetup] with the most
     *        dependencies last, and the [BlockchainSetup] with no dependencies first.
     *
     * @param sysPreSetup is the input.
     * @return a [SystemSetup]
     */
    fun buildSystemSetup(
            sysPreSetup: SystemPreSetup
    ): SystemSetup  {
        val blockchainConfList = mutableListOf<BlockchainSetup>()
        val currentBcDependencies = mutableMapOf<Int, BlockchainRelatedInfo>()
        val notFinishedPreSetups = mutableMapOf<Int, BlockchainPreSetup>()
        notFinishedPreSetups += sysPreSetup.bcPreSetups

        while (notFinishedPreSetups.isNotEmpty()) {
            val nextToBuild = findNextBcToBuild(notFinishedPreSetups, currentBcDependencies.toMap())
            val gtv = sysPreSetup.bcPreSetups[nextToBuild]!!.toGtvConfig(currentBcDependencies)
            val bcSetup = BlockchainSetupFactory.buildFromGtv(nextToBuild, gtv)
            currentBcDependencies[nextToBuild] = BlockchainRelatedInfo(bcSetup.rid, "chain_$nextToBuild", nextToBuild.toLong())
            notFinishedPreSetups.remove(nextToBuild)
            blockchainConfList.add(bcSetup)
        }

        return buildSystemSetup(blockchainConfList)
    }

    /**
     * @return the first BC we find that can be created
     */
    private fun findNextBcToBuild(notFinishedPreSetups: MutableMap<Int, BlockchainPreSetup>,
                                 currentBcDependencies: Map<Int, BlockchainRelatedInfo>): Int {
        for (chainId in notFinishedPreSetups.keys) {
            val bcPreSetup = notFinishedPreSetups[chainId]!!
            if (bcPreSetup.chainDependencies.isEmpty()) {
                return chainId
            } else if (bcPreSetup.allDependenciesMatched(currentBcDependencies.keys)) {
                return chainId // We have all dependencies we need to build GTV for this chain.
            }
        }

        throw IllegalStateException("Blockchain depend on each other in a cyclic way. Not allowed")
    }

    /**
     * Here we look at the blockchain configurations files given, and figure out what nodes we need from that.
     * The [NodeSetup] we create here will know exactly what nodes they should connect to
     *
     * @param blockchainConfFileMap holds the names of the BC conf files, connected with its chainIid.
     * @return A [SystemSetup] including everything we need for the test.
     */
    fun buildSystemSetup(
            blockchainConfFileMap: Map<Int, String>
    ): SystemSetup {
        val bcSetups = mutableListOf<BlockchainSetup>()

        for (chainId in blockchainConfFileMap.keys) {
            val filename = blockchainConfFileMap[chainId]!!
            val bcSetup = BlockchainSetupFactory.buildFromFile(chainId, filename)

            bcSetups.add(bcSetup)
        }
        return buildSystemSetup(bcSetups.toList())
    }

    /**
     * Here we look at the blockchain configurations given, and figure out what nodes we need from that.
     * The [NodeSetup] we create here will know exactly what nodes they should connect to
     *
     * @param blockchainConfList holds the complete blockchain configurations
     * @return A [SystemSetup] including everything we need for the test.
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
     * Check the blockchain Setups if this node is a signer and the BC has dep.
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