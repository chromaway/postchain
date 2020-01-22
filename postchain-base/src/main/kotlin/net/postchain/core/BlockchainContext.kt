package net.postchain.core

import net.postchain.base.BlockchainRid

/**
 * BlockchainContext interface
 *
 * @property blockchainRID globally unique identifier of a blockchain
 * @property nodeID index of a validator within signers array. For non-validator should be NODE_ID_READONLY.
 * @property chainID local identifier of a blockchain within DB, like 1, 2, 3.
 * @property nodeRID is a block signing key, it's called subjectID in other contexts.
 */
interface BlockchainContext {
    val blockchainRID: BlockchainRid
    val nodeID: Int
    val chainID: Long
    val nodeRID: ByteArray?
}
