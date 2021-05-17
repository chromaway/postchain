// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools

import mu.KLogging
import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.core.*
import java.util.concurrent.LinkedBlockingQueue

@Suppress("UNUSED_PARAMETER")
class OnDemandBlockBuildingStrategy(
        configData: BaseBlockchainConfigurationData,
        val blockchainConfiguration: BlockchainConfiguration,
        val blockQueries: BlockQueries,
        val txQueue: TransactionQueue
) : BlockBuildingStrategy {

    companion object : KLogging()

    @Volatile
    var upToHeight: Long = -1
    @Volatile
    var committedHeight = blockQueries.getBestHeight().get().toInt()
    val blocks = LinkedBlockingQueue<BlockData>()

    override fun shouldBuildBlock(): Boolean {
        //logger.debug("shouldBuildBlock() - upToHeight: $upToHeight , committedHeight: $committedHeight")
        return upToHeight > committedHeight
    }

    fun buildBlocksUpTo(height: Long) {
        logger.debug("buildBlocksUpTo() - height: $height")
        upToHeight = height
    }

    override fun blockCommitted(blockData: BlockData) {
        committedHeight++
        logger.debug("blockCommitted() - committedHeight: $committedHeight")
        blocks.add(blockData)
    }

    fun awaitCommitted(height: Int) {
        logger.debug("awaitCommitted() - start: height: $height, committedHeight: $committedHeight")
        while (committedHeight < height) {
            blocks.take()
            logger.debug("awaitCommitted() - took a block height: $height, committedHeight: $committedHeight")
        }
        var x = -2
        if (this.blockQueries != null) {
            x = blockQueries.getBestHeight().get().toInt()
        }
        logger.debug("awaitCommitted() - end: height: $height, committedHeight: $committedHeight, from db: $x")
    }

    override fun shouldStopBuildingBlock(bb: BlockBuilder): Boolean {
        return false
    }
}