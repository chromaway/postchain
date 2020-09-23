// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

import java.sql.Connection

interface AppContext {
    val conn: Connection
    fun <T> getInterface(c: Class<T>): T? {
        return null
    }
}

interface ExecutionContext : AppContext {
    val chainID: Long
    val nodeID: Int // TODO: [et]: Remove it?
}

typealias EContext = ExecutionContext

interface BlockEContext : EContext {
    val blockIID: Long
    val timestamp: Long
    fun getChainDependencyHeight(chainID: Long): Long
}

interface TxEContext : BlockEContext {
    val txIID: Long
}

/**
 * TODO: this seems out of place here, also obsolete:
1. Manager reads JSON and finds BlockchainConfigurationFactory class name.
2. Manager instantiates a class which implements BlockchainConfigurationFactory interface, and feeds it JSON data.
3. BlockchainConfigurationFactory creates BlockchainConfiguration object.
4. BlockchainConfiguration acts as a block factory and creates a transaction factory, presumably passing it configuration data in some form.
5. TransactionFactory will create Transaction objects according to configuration, possibly also passing it the configuration data.
6. Transaction object can perform its duties according to the configuration it received, perhaps creating sub-objects called transactors and passing them the configuration.
 **/


// TODO: can we generalize conn? We can make it an Object, but then we have to do typecast everywhere...

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


