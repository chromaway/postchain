package net.postchain.core

import java.sql.Connection

/**
1. Manager reads JSON and finds BlockchainConfigurationFactory class name.
2. Manager instantiates a class which implements BlockchainConfigurationFactory interface, and feeds it JSON data.
3. BlockchainConfigurationFactory creates BlockchainConfiguration object.
4. BlockchainConfiguration acts as a block factory and creates a transaction factory, presumably passing it configuration data in some form.
5. TransactionFactory will create Transaction objects according to configuration, possibly also passing it the configuration data.
6. Transaction object can perform its duties according to the configuration it received, perhaps creating sub-objects called transactors and passing them the configuration.
 **/
// TODO: can we generalize conn? We can make it an Object, but then we have to do typecast everywhere...
open class EContext(
        val conn: Connection,
        val chainID: Long,
        val nodeID: Int)

/**
 * Indicates that NodeID of a consensus group member node should be determined automatically
 * the infrastructure
 */
const val NODE_ID_AUTO = -2

/**
 * Indicates that node is should be configured as read-only replica which has no special role
 * in the consensus process and thus its identity does not matter.
 */
const val NODE_ID_READ_ONLY = -1

/**
 * Used when "node id" is not applicable to the blockchain configuration in question.
 */
const val NODE_ID_NA = -3

const val NODE_ID_TODO = -1

open class BlockEContext(
        conn: Connection,
        chainID: Long,
        nodeID: Int,
        val blockIID: Long,
        val timestamp: Long,
        val dependencyHeightMap: Map<Long, Long>)
    : EContext(conn, chainID, nodeID) {

    /**
     * @param chainID is the blockchain dependency we want to look at
     * @return the required height of the blockchain (specificied by the chainID param)
     *         or null if there is no such dependency.
     *         (Note that Height = 0 is a dependency without any blocks, which is allowed)
     */
    fun getChainDependencyHeight(chainID:Long): Long? {
        return dependencyHeightMap[chainID]
    }
}


class TxEContext(
        conn: Connection,
        chainID: Long,
        nodeID: Int,
        blockIID: Long,
        timestamp: Long,
        dependencyHeightMap: Map<Long, Long>,
        val txIID: Long)
    : BlockEContext(conn, chainID, nodeID, blockIID, timestamp, dependencyHeightMap)

