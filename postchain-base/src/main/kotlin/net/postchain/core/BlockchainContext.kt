package net.postchain.core

interface BlockchainContext {
    val blockchainRID: ByteArray
    val nodeID: Int
    val chainID: Long
    val nodeRID: ByteArray?
}
