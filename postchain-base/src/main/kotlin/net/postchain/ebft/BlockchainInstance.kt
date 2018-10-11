package net.postchain.ebft

import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.BlockchainProcess

/**
 * A blockchain instance model
 * @property blockchainConfiguration stateless object which describes an individual blockchain instance
 * @property blockDatabase wrapper class for the [engine] and [blockQueries], starting new threads when running
 * @property blockManager manages intents and acts as a wrapper for [blockDatabase] and [statusManager]
 * @property statusManager manages the status of the consensus protocol
 * @property syncManager
 * @property networkAwareTxQueue
 */
interface BlockchainInstanceModel : BlockchainProcess {
    val blockchainConfiguration: BlockchainConfiguration
    val blockDatabase: BaseBlockDatabase
    val blockManager: BlockManager
    val statusManager: BaseStatusManager
    val syncManager: SyncManager
    val networkAwareTxQueue: NetworkAwareTxQueue
}
