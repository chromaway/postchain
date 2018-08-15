package net.postchain.test

import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.core.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("UNUSED_PARAMETER")
class OnDemandBlockBuildingStrategy(configData: BaseBlockchainConfigurationData,
                                    val blockchainConfiguration: BlockchainConfiguration,
                                    blockQueries: BlockQueries, val txQueue: TransactionQueue)
    : BlockBuildingStrategy {
    val triggerBlock = AtomicBoolean(false)
    val blocks = LinkedBlockingQueue<BlockData>()
    var committedHeight = -1
    override fun shouldBuildBlock(): Boolean {
        return triggerBlock.getAndSet(false)
    }

    fun triggerBlock() {
        triggerBlock.set(true)
    }

    override fun blockCommitted(blockData: BlockData) {
        blocks.add(blockData)
    }

    fun awaitCommitted(blockHeight: Int) {
        while (committedHeight < blockHeight) {
            blocks.take()
            committedHeight++
        }
    }

    override fun shouldStopBuildingBlock(bb: BlockBuilder): Boolean {
        return false
    }
}