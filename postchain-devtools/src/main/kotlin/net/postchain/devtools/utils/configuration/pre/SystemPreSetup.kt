package net.postchain.devtools.utils.configuration.pre

import net.postchain.devtools.utils.configuration.*

/**
 * This setup holds the "pre" system setup (holding data that's used before the [SystemSetup] can be built)
 * This class is used for test where we don't have a blockchain configuration files for our test, and need to generate the GTV configuration.
 *
 * (The "setup" classes are data holders/builders for test configuration used to generate the "real" classes at a later stage)
 *
 * @property bcPreSetups is a map that holds the primitive blockchain setups
 */
class SystemPreSetup(
        val bcPreSetups: Map<Int, BlockchainPreSetup>
) {


    companion object {
        /**
         * This builder builds a simple setup where all nodes are configured the same way
         * (= all of them running all the blockchains).
         *
         * @param nodeCount is the number of nodes we should use in our test.
         * @param blockchainList contains the blockchains we will use in our test.
         */
        fun simpleBuild(
                nodeCount: Int,
                blockchainList: List<Int>
        ): SystemPreSetup {
            val retBlockchainMap = mutableMapOf<Int, BlockchainPreSetup>()

            // 1. Build a list of all nodes
            val tmpNodeNrList = mutableListOf<NodeSeqNumber>()
            for (i in 0 until nodeCount) {
                tmpNodeNrList.add(NodeSeqNumber(i))
            }
            val nodeNrList = tmpNodeNrList.toList()
            val bcsToSign = mutableSetOf<Int>()

            // 2. Create the blockchains (must do this before the nodes or it'll break!!)
            for (chainId in blockchainList) {
                retBlockchainMap[chainId] = BlockchainPreSetup.simpleBuild(chainId, nodeNrList)
                bcsToSign.add(chainId)
            }

            return SystemPreSetup(retBlockchainMap.toMap())
        }
    }
}