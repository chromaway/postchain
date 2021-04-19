// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft

import mu.KLogging
import net.postchain.common.toHex
import net.postchain.core.BlockBuildingStrategy
import net.postchain.core.BlockData
import net.postchain.core.BlockDataWithWitness
import net.postchain.core.PmEngineIsAlreadyClosed
import net.postchain.debug.BlockchainProcessName
import nl.komponents.kovenant.Promise

/**
 * Manages intents and acts as a wrapper for [blockDatabase] and [statusManager]
 */
class BaseBlockManager(
        private val processName: BlockchainProcessName,
        val blockDB: BlockDatabase,
        val statusManager: StatusManager,
        val blockStrategy: BlockBuildingStrategy
) : BlockManager {

    @Volatile
    var processing = false

    @Volatile
    var intent: BlockIntent = DoNothingIntent

    companion object : KLogging()

    @Volatile
    override var currentBlock: BlockData? = null

    protected fun <RT> runDBOp(op: () -> Promise<RT, Exception>, onSuccess: (RT) -> Unit, onFailure: (Exception) -> Unit = {}) {
        if (!processing) {
            synchronized(statusManager) {
                processing = true
                intent = DoNothingIntent

                op() success { res ->
                    synchronized(statusManager) {
                        onSuccess(res)
                        processing = false
                    }
                } fail { err ->
                    synchronized(statusManager) {
                        onFailure(err)
                        processing = false
                        logger.debug("Error in runDBOp()", err)
                    }
                }
            }
        }
    }

    override fun onReceivedUnfinishedBlock(block: BlockData) {
        synchronized(statusManager) {
            val theIntent = intent
            if (theIntent is FetchUnfinishedBlockIntent && theIntent.blockRID.contentEquals(block.header.blockRID)) {
                runDBOp({
                    blockDB.loadUnfinishedBlock(block)
                }, { signature ->
                    if (statusManager.onReceivedBlock(block.header.blockRID, signature)) {
                        currentBlock = block
                    }
                }, { exception ->
                    val msg = "$processName: Can't load unfinished block ${theIntent.blockRID.toHex()}: " +
                            "${exception.message}"
                    if (exception is PmEngineIsAlreadyClosed) {
                        logger.debug(msg)
                    } else {
                        logger.error(msg)
                    }
                })
            }
        }
    }

    override fun onReceivedBlockAtHeight(block: BlockDataWithWitness, height: Long) {
        synchronized(statusManager) {
            val theIntent = intent
            if (theIntent is FetchBlockAtHeightIntent && theIntent.height == height) {
                runDBOp({
                    blockDB.addBlock(block)
                }, {
                    if (statusManager.onHeightAdvance(height + 1)) {
                        currentBlock = null
                    }
                }, { exception ->
                    val msg = "$processName: Can't add received block ${block.header.blockRID.toHex()} " +
                            "at height $height: ${exception.message}"
                    if (exception is PmEngineIsAlreadyClosed) {
                        logger.debug(msg)
                    } else {
                        logger.error(msg)
                    }
                })
            }
        }
    }

    // this is called only in getBlockIntent which is synchronized on status manager
    protected fun update() {
        if (processing) return
        val blockIntent = statusManager.getBlockIntent()
        intent = DoNothingIntent
        when (blockIntent) {

            is CommitBlockIntent -> {
                if (currentBlock == null) {
                    logger.error("$processName: Don't have a block StatusManager wants me to commit")
                    return
                }
                runDBOp({
                    blockDB.commitBlock(statusManager.commitSignatures)
                }, {
                    statusManager.onCommittedBlock(currentBlock!!.header.blockRID)
                    currentBlock = null
                }, { exception ->
                    logger.error("$processName: Can't commit block ${currentBlock!!.header.blockRID.toHex()}: " +
                            "${exception.message}")
                })
            }

            is BuildBlockIntent -> {
                // It's our turn to build a block. But we need to consult the
                // BlockBuildingStrategy in order to figure out if this is the
                // right time. For example, the strategy may decide that
                // we won't build a block until we have at least three transactions
                // in the transaction queue. Or it will only build a block every 10 minutes.
                // Be careful not to have a BlockBuildingStrategy that conflicts with the
                // RevoltTracker of ValidatorSyncManager.
                if (!blockStrategy.shouldBuildBlock()) {
                    return
                }
                runDBOp({
                    blockDB.buildBlock()
                }, { blockAndSignature ->
                    val block = blockAndSignature.first
                    val signature = blockAndSignature.second
                    if (statusManager.onBuiltBlock(block.header.blockRID, signature)) {
                        currentBlock = block
                    }
                }, { exception ->
                    val msg = "$processName: Can't build block at height ${statusManager.myStatus.height + 1}: ${exception.message}"
                    if (exception is PmEngineIsAlreadyClosed) {
                        logger.debug(msg)
                    } else {
                        logger.error(msg)
                    }
                })
            }

            else -> intent = blockIntent
        }
    }


    override fun isProcessing(): Boolean {
        return processing
    }

    override fun getBlockIntent(): BlockIntent {
        synchronized(statusManager) {
            update()
        }
        return intent
    }
}