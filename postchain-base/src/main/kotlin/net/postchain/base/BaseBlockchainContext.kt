package net.postchain.base

import net.postchain.core.BlockchainContext

open class BaseBlockchainContext(
        override val blockchainRID: ByteArray,
        override val nodeID: Int,
        override val chainID: Long,
        override val nodeRID: ByteArray?)
    : BlockchainContext

