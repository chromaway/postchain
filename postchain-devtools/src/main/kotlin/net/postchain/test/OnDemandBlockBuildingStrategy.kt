package net.postchain.test

import mu.KLogging
import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.core.*
import java.util.concurrent.LinkedBlockingQueue

@Suppress("UNUSED_PARAMETER")
class OnDemandBlockBuildingStrategy(
        configData: BaseBlockchainConfigurationData,
        val blockchainConfiguration: BlockchainConfiguration,
        blockQueries: BlockQueries,
        val txQueue: TransactionQueue
) : BlockBuildingStrategy {

    companion object : KLogging()

    @Volatile
    var upToHeight : Long = -1

    val blocks = LinkedBlockingQueue<BlockData>()
    @Volatile
    var committedHeight = -1

    override fun shouldBuildBlock(): Boolean {
        return upToHeight > committedHeight
    }

    fun buildBlocksUpTo(j: Long) {
        upToHeight = j
    }

    override fun blockCommitted(blockData: BlockData) {
        committedHeight++
        blocks.add(blockData)
    }

    fun awaitCommitted(blockHeight: Int) {
        while (committedHeight < blockHeight) {
            blocks.take()
        }
    }

    override fun shouldStopBuildingBlock(bb: BlockBuilder): Boolean {
        return false
    }
}