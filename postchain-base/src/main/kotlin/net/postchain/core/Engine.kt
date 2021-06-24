// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

import net.postchain.base.BlockchainRid
import net.postchain.debug.BlockTrace

interface Shutdownable {
    fun shutdown()
}

interface Synchronizable {
    val synchronizer: Any
}

/**
 * Blockchain engine used for building and adding new blocks
 */
interface BlockchainEngine : Shutdownable {
    fun isRunning(): Boolean
    fun initialize()
    fun setRestartHandler(restartHandler: RestartHandler)

    fun loadUnfinishedBlock(block: BlockData): Pair<ManagedBlockBuilder, Exception?>
    fun buildBlock(): Pair<ManagedBlockBuilder, Exception?>
    fun getTransactionQueue(): TransactionQueue
    fun getBlockBuildingStrategy(): BlockBuildingStrategy
    fun getBlockQueries(): BlockQueries
    fun getConfiguration(): BlockchainConfiguration
}

interface BlockchainProcess {
    fun getEngine(): BlockchainEngine
    fun shutdown()
}

interface BlockchainProcessManager : Shutdownable, Synchronizable {
    fun startBlockchain(chainId: Long, bTrace: BlockTrace?): BlockchainRid?
    fun retrieveBlockchain(chainId: Long): BlockchainProcess?
    fun stopBlockchain(chainId: Long, bTrace: BlockTrace?, restart: Boolean = false)
}

// A return value of "true" means a restart is needed.
typealias RestartHandler = (BlockTrace?) -> Boolean

