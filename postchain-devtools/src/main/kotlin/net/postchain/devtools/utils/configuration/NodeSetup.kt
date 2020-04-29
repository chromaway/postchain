package net.postchain.devtools.utils.configuration

import mu.KLogging
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.devtools.KeyPairHelper
import net.postchain.devtools.PostchainTestNode


/**
 * Represents a single test node
 * (The "setup" classes are data holders/builders for test configuration used to generate the "real" classes at a later stage)
 *
 * @property sequenceNumber is the node's number. Must be unique.
 * @property chainsToSign the blockchains this node should be a signer for (SET because no duplicates allowed)
 * @property chainsToRead the blockchains this node should have a read only copy of (SET because no duplicates allowed)
 * @property pubKeyHex is the pub key
 * @property privKeyHex is the private key
 * @property configurationProvider is the configuration provider for the node
 */
data class NodeSetup(
        val sequenceNumber: NodeSeqNumber,
        val chainsToSign: Set<Int>,
        val chainsToRead: Set<Int>,
        val pubKeyHex: String,
        val privKeyHex: String,
        var configurationProvider: NodeConfigurationProvider? = null // We might not set this at first
) {

    companion object: KLogging() {
        const val DEFAULT_PORT_BASE_NR= 9870 // Just some made up number. Can be used if there is no other test running in parallel on this machine.
        const val DEFAULT_API_PORT_BASE = 7740 // Made up number, used for the REST API

        fun buildSimple(nodeNr: NodeSeqNumber,
                        signerChains: Set<Int>,
                        replicaChains: Set<Int>
                        ): NodeSetup {

            return NodeSetup(
                    nodeNr,
                    signerChains,
                    replicaChains,
                    KeyPairHelper.pubKeyHex(nodeNr.nodeNumber),
                    KeyPairHelper.privKeyHex(nodeNr.nodeNumber)
            )
        }
    }

    fun getAllBlockchains() = this.chainsToSign.plus(this.chainsToRead)

    /**
     * The nodes have ports depending on the order of their sequence number
     */
    fun getPortNumber() = getPortNumber(DEFAULT_PORT_BASE_NR)
    fun getPortNumber(portBase: Int) = sequenceNumber.nodeNumber + portBase

    fun getApiPortNumber() = getApiPortNumber(DEFAULT_API_PORT_BASE)
    fun getApiPortNumber(portBase: Int) = sequenceNumber.nodeNumber + portBase // Must be different from "normal" port base

    /**
     * It can be pretty hard to figure out if a node needs to know about some other node,
     * but we have done most of the work already since we know all the BC's this node is holding.
     *
     * @param systemSetup
     * @return all nodes this node should know about
     */
    fun calculateAllNodeConnections(systemSetup: SystemSetup): Set<NodeSeqNumber>
    {
        val retSet = mutableSetOf<NodeSeqNumber>()

        val totalBcs = this.chainsToRead.plus(this.chainsToRead)
        for (chainId in totalBcs) {
            val bc = systemSetup.blockchainMap[chainId]!!
            retSet.addAll(bc.signerNodeList) //  should connect to all others
        }

        // Just remove ourselves and return
        return retSet.filter { it != this.sequenceNumber }.toSet()
    }

    /**
     * Turns this [NodeSetup] to a [PostchainTestNode] and adds and starts all blockchains on it
     */
    fun toTestNodeAndStartAllChains(
            systemSetup: SystemSetup,
            preWipeDatabase: Boolean = true
    ): PostchainTestNode {

        require (configurationProvider != null) {"Cannot build a PostchainTestNode without a NodeConfigurationProvider set"}

        val x = configurationProvider!!.getConfiguration()
        //logger.debug("Peers: ${x.peerInfoMap.size}")
        val node = PostchainTestNode(configurationProvider!!, preWipeDatabase)


        if (chainsToRead.isNotEmpty()) {
            logger.debug("Node ${sequenceNumber.nodeNumber}: Start all read only blockchains (dependencies must be installed first)")
            // TODO: These chains can in turn be depending on each other, so they should be "sorted" first
            chainsToRead.forEach { chainId ->
                val chainSetup = systemSetup.blockchainMap[chainId]!!
                logger.debug("Node ${sequenceNumber.nodeNumber}: Begin starting read only chainId: $chainId ")
                node.addBlockchain(chainSetup)
                node.mapBlockchainRID(chainId.toLong(), chainSetup.rid)
                node.startBlockchain(chainId.toLong())
                logger.debug("Node ${sequenceNumber.nodeNumber}: Finished starting read only chainId: $chainId ")
            }
        }

        logger.debug("Node ${sequenceNumber.nodeNumber}: Start all blockchains we should sign")
        chainsToSign.forEach {chainId ->
            val chainSetup = systemSetup.blockchainMap[chainId]!!
            logger.debug("Node ${sequenceNumber.nodeNumber}: Begin starting chainId: $chainId ")
            node.addBlockchain(chainSetup)
            node.mapBlockchainRID(chainId.toLong(), chainSetup.rid)
            node.startBlockchain(chainId.toLong())
            logger.debug("Node ${sequenceNumber.nodeNumber}: Finished starting chainId: $chainId ")
        }

        return node
    }

}

