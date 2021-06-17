// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import mu.KLogging
import net.postchain.common.data.Hash
import net.postchain.common.toHex
import net.postchain.core.ProgrammerMistake


/**
 * Describes the blockchain
 *
 * @property blockchainRid is the external (real) key of the BC
 * @property nickname is a human readable name for this blockchain
 * @property chainId is the internal ID of the BC
 */
data class BlockchainRelatedInfo(
        val blockchainRid: BlockchainRid,
        val nickname: String?,
        var chainId: Long? // We can set this one later
) {

    // Only for error messages etc
    override fun toString(): String {
        val nic = if (nickname != null) {
            "name: $nickname, "
        } else {
            ""
        }

        return "$nic , ${blockchainRid.toHex()}"
    }
}

/**
 * Describes the block height of the blockchain
 *
 * @property lastBlockRid is the RID of the last block
 * @property height is the height of the last block (not really used for anything but debugging, so can be scrapped)
 */
data class HeightDependency(
        val lastBlockRid: Hash,
        val height: Long? ) {

    // Only for error messages etc
    override fun toString(): String {
        val h = if (height != null) {
            "height: $height,"
        } else {
            ""
        }
        return "$h last_block_RID: ${lastBlockRid.toHex()}"
    }
}



/**
 * Maps a blockchain to it's latest block.
 *
 * @property blockchainRelatedInfo
 * @property heightDependency is the height of the BC, (The idea is that we can be set this later or update the height of this object if needed)
 */
data class BlockchainDependency(
        val blockchainRelatedInfo: BlockchainRelatedInfo,
        val heightDependency: HeightDependency?) {

    // Only for error messages etc
    override fun toString(): String {
        return "$blockchainRelatedInfo ( ${heightDependency ?: ""} )"
    }
}

/**
 * Holds a set of [BlockchainDependency] with the chainId as a key.
 * Purpose: to keep track of all dependencies a BC has.
 *
 * Note: The reason we have two internal maps is that we want to be ably to lookup the dependency in two ways.
 *
 * @property internalArray holds the data in the same order as found in the configuration
 */
class BlockchainDependencies(
        private val internalArray: Array<BlockchainDependency>
) {
    private val chaindIdMap = mutableMapOf<Long, BlockchainDependency>() // Convenience lookups
    private val blockchainRidMap = mutableMapOf<BlockchainRid, BlockchainDependency>() // Convenience lookups

    companion object : KLogging() {

        const val NO_BLOCKS_YET = -1L // We cannot use 0, bc that is the height of the first block.
    }

    // Convenience constructor
    constructor(depList: List<BlockchainDependency>): this(depList.toTypedArray()) {
        for (dep in depList) {
            add(dep)
        }
    }

    // ------------------------------------------------
    // Collection functions
    // ------------------------------------------------
    fun isEmpty() = internalArray.isEmpty()
    fun all(): List<BlockchainDependency> = internalArray.toList()

    operator fun get(index: Int): BlockchainDependency = internalArray[index]

    fun getFromChainId(chainId: Long): BlockchainDependency? = chaindIdMap[chainId]
    fun getFromBlockchainRID(bcRid: BlockchainRid): BlockchainDependency? = blockchainRidMap[bcRid]

    fun add(dep: BlockchainDependency) {
        if (dep.blockchainRelatedInfo.chainId != null) {
            chaindIdMap[dep.blockchainRelatedInfo.chainId!!] = dep
        }
        blockchainRidMap[dep.blockchainRelatedInfo.blockchainRid] = dep
    }


    // ------------------------------------------------
    // Other functions
    // ------------------------------------------------

    fun isDependingOnBlockchain(chainId: Long): Boolean = chaindIdMap.containsKey(chainId)

    /**
     * Extracts a map ChainID -> blockchain height from the data.
     * Note that this requires the data to be complete, or else we will explode.
     */
    fun extractChainIdToHeightMap(): Map<Long, Long> {
        val retMap = mutableMapOf<Long, Long>()
        for (dep in internalArray) {
            val chainId = dep.blockchainRelatedInfo.chainId ?: throw ProgrammerMistake("Must have a chainId for $dep")
            val height = getHeight(dep)
            retMap[chainId] = height
        }
        return retMap.toMap()
    }

    /**
     * @return the height of the dependency.
     *
     * If there is no height yet we will assume 0.
     * This was decided after discussion with Alex: we allow this to make testing easier, but in "real world" a
     * blockchain we depend on should have at least a genesis block.
     */
    private fun getHeight(dep: BlockchainDependency): Long {
        if (dep.heightDependency != null) {
            logger.debug("Have height for dependency: $dep")
            return dep.heightDependency.height ?:
               throw ProgrammerMistake("If last block RID exists for dependency: $dep we must know height at this stage")
        } else {
            logger.warn("No height known for dependency $dep, so assuming height = $NO_BLOCKS_YET. (This is ok if we are running a test)")
            return NO_BLOCKS_YET
        }

    }

    fun extractBlockHeightDependencyArray(): Array<Hash?>? {
        return if (isEmpty()) {
            null
        } else {
            internalArray.map { it ->
                if (it.heightDependency != null) {
                    it.heightDependency.lastBlockRid
                } else {
                    null
                }
            }.toTypedArray()
        }
    }
}