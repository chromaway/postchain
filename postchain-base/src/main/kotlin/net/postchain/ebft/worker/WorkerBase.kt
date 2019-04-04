package net.postchain.ebft.worker

import net.postchain.base.NetworkAwareTxQueue
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.BlockchainEngine
import net.postchain.core.BlockchainProcess
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.syncmanager.SyncManagerBase

/**
 * A blockchain instance model
 * @property blockchainConfiguration stateless object which describes an individual blockchain instance
 * @property blockDatabase wrapper class for the [BlockchainEngine] and [BlockQueries], starting new threads when running
 * @property syncManager
 * @property networkAwareTxQueue
 */
interface WorkerBase : BlockchainProcess {
    val blockchainConfiguration: BlockchainConfiguration
    val blockDatabase: BaseBlockDatabase
    val syncManager: SyncManagerBase
    val networkAwareTxQueue: NetworkAwareTxQueue
}
