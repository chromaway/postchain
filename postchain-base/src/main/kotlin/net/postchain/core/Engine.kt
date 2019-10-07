package net.postchain.core

import java.util.concurrent.locks.Lock

interface Shutdownable {
    fun shutdown()
}

interface Synchronizable {
    var synchronizer: Lock
}

/**
 * Blockchain engine used for building and adding new blocks
 */
interface BlockchainEngine : Shutdownable {
    fun initializeDB()
    fun setRestartHandler(restartHandler: RestartHandler)
    fun addBlock(block: BlockDataWithWitness)
    fun loadUnfinishedBlock(block: BlockData): ManagedBlockBuilder
    fun buildBlock(): ManagedBlockBuilder
    fun getTransactionQueue(): TransactionQueue
    fun getBlockBuildingStrategy(): BlockBuildingStrategy
    fun getBlockQueries(): BlockQueries
    fun getConfiguration(): BlockchainConfiguration
}

interface BlockchainProcess : Shutdownable {
    fun getEngine(): BlockchainEngine
}

interface BlockchainProcessManager : Shutdownable, Synchronizable {
    fun startBlockchainAsync(chainId: Long)
    fun startBlockchain(chainId: Long)
    fun retrieveBlockchain(chainId: Long): BlockchainProcess?
    fun stopBlockchain(chainId: Long)
    fun getBlockchains(): Set<Long>
}

typealias RestartHandler = () -> Boolean
