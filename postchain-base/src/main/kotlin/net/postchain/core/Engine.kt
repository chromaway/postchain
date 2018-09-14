package net.postchain.core

interface Shutdownable {
    fun shutdown()
}

interface BlockchainEngine : Shutdownable {
    fun initializeDB()
    fun addBlock(block: BlockDataWithWitness)
    fun loadUnfinishedBlock(block: BlockData): ManagedBlockBuilder
    fun buildBlock(): ManagedBlockBuilder
    fun getTransactionQueue(): TransactionQueue
    fun getBlockBuildingStrategy(): BlockBuildingStrategy
    fun getBlockQueries(): BlockQueries
    fun getConfiguration(): BlockchainConfiguration
    fun setRestartHandler(restartHandler: RestartHandler)
}

interface BlockchainProcess : Shutdownable {
    fun getEngine(): BlockchainEngine
}

interface BlockchainProcessManager : Shutdownable {
    fun startBlockchain(chainId: Long)
    fun retrieveBlockchain(chainId: Long): BlockchainProcess?
}

typealias RestartHandler = () -> Unit
