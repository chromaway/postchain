package net.postchain.devtools.utils.configuration

import net.postchain.base.PeerInfo
import net.postchain.common.hexStringToByteArray
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.ByteArrayKey
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.PostchainTestNode
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.devtools.utils.configuration.pre.SystemPreSetup


/**
 * Describes the architecture for a "system" of test nodes, running one or more dapps.
 * A "system" in this case includes a group of test nodes and test blockchains where:
 *
 * 1. all nodes run on the same machine,
 * 2. all nodes have standard "test" port numbers
 *
 * [SystemSetup] is not a stand alone test framework, but meant to be used by subclasses of [IntegrationTestSetup] or similar.
 *
 * [SystemSetup] works like a builder, we add data to it (mostly other "setup" objects) and finally calls "toTestNodes()"
 *   which corresponds to the "build()" function in the builder pattern.
 *
 * Note: When using [SystemSetup] for your test setup it doesn't matter if you have a node configuration file or not
 *  (for example "node1.properties"). Since the [SystemSetup] will use the [NodeConfigurationProvider] instance sent to it
 *  as an argument in the "toTestNodes()" function, it doesn't matter how the [NodeConfigurationProvider] was created.
 *  However, the most common thing would be to use "buildFromSetup()" in [NodeConfigurationProviderGenerator] to
 *  simulate the node conf file, which means that you don't have to provide these files for your test.

 *
 * @property nodeMap is a map from node seq number to [NodeSetup]. Represents all nodes in the system.
 * @property blockchainMap is a map from chain ID to [BlockchainSetup]. Represents all BCs in the system.
 * @property realGtxTransactions is true if the test should produce real [GTXTransaction]. This is default
 * @property nodeConfProvider is a node configuration attribute (present in the node's conf file). We use "legacy" as default.
 * @property chainConfProvider is a node configuration attribute (present in the node's conf file). (NOTE: Putting this
 *                             setting here means that we cannot create a test with different chain conf providers in
 *                             different nodes, but it's (probably) not a common setup anyway)
 * @property confInfrastructure is a node configuration attribute (present in the node's conf file). We use "base/ebft" as default.
 */
data class SystemSetup(
        val nodeMap: Map<NodeSeqNumber, NodeSetup>,
        val blockchainMap: Map<Int, BlockchainSetup>,

        // Default configurations are set to make the test as realistic as possible.
        var realGtxTransactions: Boolean = true,
        var nodeConfProvider: String = "legacy", // Override with "managed" if that's what you need.
        var chainConfProvider: String = "manual", // Override with "managed" if that's what you need.
        var confInfrastructure: String = "base/ebft", // Override with "base/test" if you don't need real consensus.
        var needRestApi: Boolean = false // If this is set to true, all nodes will start the REST API (default is "false" since most test don't use the API)
) {

    companion object {

        /**
         * This builder builds a simple setup where all nodes are configured the same way
         * (= all of them running all the blockchains).
         *
         * @param nodeCount is the number of nodes we should use in our test.
         * @param blockchainList contains the blockchains we will use in our test.
         */
        fun buildSimpleSetup(
                nodeCount: Int,
                blockchainList: List<Int> = listOf(1) // as default we assume just one blockchain
        ): SystemSetup = SystemSetupFactory.buildSystemSetup(SystemPreSetup.simpleBuild(nodeCount, blockchainList))


        /**
         * Complex setup means we look at the blockchain configurations given, and figure out what nodes we need from that.
         * The [NodeSetup] we will create will know exactly what nodes they should connect to
         *
         * @param blockchainConfList holds the complete blockchain configurations
         */
        fun buildComplexSetup(
                blockchainConfList: List<BlockchainSetup>
        ): SystemSetup  =  SystemSetupFactory.buildSystemSetup(blockchainConfList)



        /**
         * Can be used to modify the node map of a setup.
         *
         * (Note: since the setup is immutable we do it this way, maybe too strict and mutable collections could be used?)
         *
         * @param newNodeSetups are the nodes to add
         * @param oldSysSetup is the previous setup
         * @return a new immutable system setup with the new nodes added
         */
        fun addNodesToSystemSetup(newNodeSetups: Map<NodeSeqNumber, NodeSetup>,
                                  oldSysSetup: SystemSetup
        ): SystemSetup {

            // Merge the maps
            val nodesMap = mutableMapOf<NodeSeqNumber, NodeSetup>()
            nodesMap.putAll(oldSysSetup.nodeMap)
            nodesMap.putAll(newNodeSetups)

            // We are just returning a new instance with everything the same except for the nodeMap.
            return SystemSetup(nodesMap,
                    oldSysSetup.blockchainMap,
                    oldSysSetup.realGtxTransactions,
                    oldSysSetup.nodeConfProvider,
                    oldSysSetup.chainConfProvider,
                    oldSysSetup.confInfrastructure)
        }
    }


    /**
     * Will convert the [NodeSetup] list to list of [PeerInfo].
     */
    fun toPeerInfoList(): List<PeerInfo> {
        val peerInfos = mutableListOf<PeerInfo>()
        for (node in this. nodeMap.values) {
            val key = ByteArrayKey(node.pubKeyHex.hexStringToByteArray())
            val pi = PeerInfo("localhost", node.getPortNumber(), key)
            peerInfos.add(pi)
        }

        return peerInfos.toList()
    }

    /**
     * Get all [BlockchainSetup] that a node should sign
     */
    fun getBlockchainsANodeShouldRun(nodeNr: NodeSeqNumber): List<BlockchainSetup> {
        return blockchainMap.values.filter { bc -> bc.signerNodeList.contains(nodeNr) }
    }

    /**
     * Transform this [SystemSetup] instance into a list of [PostchainTestNode] s.
     */
    fun toTestNodes(): List<PostchainTestNode> {
        val retList = mutableListOf<PostchainTestNode>()
        for (nodeSetup in nodeMap.values) {
            val postchainNode = nodeSetup.toTestNodeAndStartAllChains(this)
            retList.add(postchainNode)
        }
        return retList
    }
}


