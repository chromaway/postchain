// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.snapshot

import net.postchain.base.data.DatabaseAccess
import net.postchain.common.data.Hash
import net.postchain.core.BlockEContext

class LeafStore {

    /**
     * Stores the event
     *
     * @param blockEContext is the context (including block height)
     * @param hash is the hash of the data
     * @param data is the binary data
     */
    fun writeEvent(blockEContext: BlockEContext, hash: Hash, data: ByteArray) {
        val db = DatabaseAccess.of(blockEContext)
        db.insertEvent(blockEContext, blockEContext.height, hash, data)
    }

    /**
     * Stores the state
     *
     * @param blockEContext is the context (including block height)
     * @param state_n // TODO
     * @param data is the binary data
     */
    fun writeState(blockEContext: BlockEContext, state_n: Long, data: ByteArray) {
        val db = DatabaseAccess.of(blockEContext)
        db.insertState(blockEContext, blockEContext.height, state_n, data)
    }

    // delete events at and below given height
    fun pruneEvents(blockEContext: BlockEContext, height: Long) {
        TODO()
    }

    // delete all states such that state_n is between left and right and state_height <= height
    fun pruneStates(blockEContext: BlockEContext, left: Long, right: Long, height: Long) {
        TODO()
    }
}