package net.postchain.debug

import mu.KLogging
import net.postchain.common.toHex

/**
 * READ THIS BEFORE DELETING THIS CLASS!
 *
 * Some synchronization errors won't show up in the debugger because they happen too seldom, so the logfile is the
 * only way to track them down.
 *
 * This is a collection of various info we know about the block, valuable for test where we
 * run multiple nodes and it's not clear from the logs WHY a thread is doing what it is doing.
 *
 * Example to be more specific:
 * For historic sync the block construction passes through 3 threads, typically:
 *
 *  1. [historicSync-X] (where we attempt to add a block) ->
 *  2. [-1-BaseBlockDatabaseWorker] (running BlockDatabase.addBlock()) Note1: Normal logging doesn't print in here, not sure why? ->
 *  3. [-1-BaseBlockDatabaseWorker] (running BlockBuilder.commit()) Note2: But in here the regular logging works again. ->
 *  4. [pool-4-thread-1] (running ProcMan, handling potential restart)
 *
 * When we are at step 4 it's usually pretty difficult to see what block caused the restart.
 * This object works as an "external ID" (often used to track flow between different apps).
 *
 * Thing about performance
 * -----------------------
 * To avoid performance hit on a production system, this object should never be created, and thus always be "null"
 * unless "DEBUG" or lower level of logging has been turned on.
 *
 * Incomplete data
 * ---------------
 * Often we don't have all info at once, so it has to be collected over time.
 * Example: Initially, in [PostchainNode] we might only know the chainId and node's PubKey.
 *          Later, in the [BaseBlockManager] we know the block we are building, so we add that info.
 */
class BlockTrace(
        var procName: BlockchainProcessName?,  // What node and blockchain is building this block
        var blockRid: String?, // What block
        var height: Long?) { // What height

    companion object : KLogging() {
        fun build(procName: BlockchainProcessName?, blockRid: ByteArray?, height: Long?): BlockTrace {
            return BlockTrace(procName, blockRid?.toHex(), height)
        }

        /**
         * Sometimes we start before we have the block
         */
        fun buildBeforeBlock(procName: BlockchainProcessName?, height: Long?): BlockTrace {
            return BlockTrace(procName, null , height) // We cannot know the Block RID before we start
        }

        /**
         * Sometimes we don't even have the height
         */
        fun buildBeforeBlock(procName: BlockchainProcessName): BlockTrace {
            return buildBeforeBlock(procName, null)
        }
    }

    /**
     * Adds missing data to the existing instance
     */
    fun addDataIfMissing(other: BlockTrace?) {

        if (other != null) {
            if (procName != null) {
                if (other.procName != null) {
                    if (procName != other.procName) {
                        logger.debug("Why do node/BC info differ? me: ${toString()}, they: $other" )
                    } else {
                        // Same, do nothing
                    }

                }
            } else if (other.procName != null) {
                procName = other.procName
            }

            // Block RID
            if (blockRid != null) {
                if (other.blockRid != null) {
                    if (blockRid != other.blockRid) {
                        logger.debug("Why do block RID differ? me: ${toString()}, they: $other" )
                    } else {
                        //Same, do nothing
                    }
                }
            } else if (other.blockRid != null) {
                blockRid = other.blockRid
            }

            // Height
            if (height != null) {
                if (other.height != null) {
                    if (height != other.height) {
                        logger.debug("Why do block height differ? me: ${toString()}, they: $other" )
                    } else {
                        //Same, do nothing
                    }
                }
            } else if (other.height != null) {
                height = other.height
            }

        }
    }

    /**
     * To avoid confusion with BC RID we use "123:456" format instead of "12:34"
     */
    fun shortBlockRid(): String = if (blockRid != null) {
        "${blockRid!!.take(3)}:${blockRid!!.takeLast(3)}"
    } else {
        "(no block RID)"
    }

    override fun toString(): String {
        return "$procName, height: $height, block: ${shortBlockRid()}"
    }
}