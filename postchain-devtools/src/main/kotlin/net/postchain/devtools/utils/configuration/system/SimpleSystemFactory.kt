package net.postchain.devtools.utils.configuration.system

import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.NodeSeqNumber
import net.postchain.devtools.utils.configuration.NodeSetup
import net.postchain.devtools.utils.configuration.SystemSetup

object SimpleSystemFactory {

    /**
     * This builder builds a simple setup where all nodes are configured the same way
     * (= all of them running all the blockchains).
     *
     * @param nodeCount is the number of nodes we should use in our test.
     * @param blockchainList contains the blockchains we will use in our test.
     */
    fun buildSystemSetup(
            nodeCount: Int,
            blockchainList: List<Int>
    ): SystemSetup {
        val retNodeMap = mutableMapOf<NodeSeqNumber, NodeSetup>()
        val retBlockchainMap = mutableMapOf<Int, BlockchainSetup>()

        // 1. Build a list of all nodes
        val tmpNodeNrList = mutableListOf<NodeSeqNumber>()
        for (i in 0 until nodeCount) {
            tmpNodeNrList.add(NodeSeqNumber(i))
        }
        val nodeNrList = tmpNodeNrList.toList()
        val bcsToSign = mutableSetOf<Int>()

        // 2. Create the blockchains (must do this before the nodes or it'll break!!)
        for (chainId in blockchainList) {
            retBlockchainMap[chainId] = BlockchainSetup.simpleBuild(chainId, nodeNrList)
            bcsToSign.add(chainId)
        }

        // 3. Create the nodes
        for (nodeNr in nodeNrList) {
            val ns = NodeSetup.buildSimple(nodeNr, bcsToSign, setOf())
            retNodeMap[nodeNr] = ns
        }

        return SystemSetup(
                retNodeMap.toMap(),
                retBlockchainMap.toMap())
    }
}