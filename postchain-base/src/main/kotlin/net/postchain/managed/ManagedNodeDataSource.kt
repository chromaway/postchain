package net.postchain.managed

import net.postchain.config.node.PeerInfoDataSource
import net.postchain.core.EContext

interface ManagedNodeDataSource : PeerInfoDataSource {
    fun getPeerListVersion(): Long
    fun computeBlockchainList(ctx: EContext): List<ByteArray>
    fun getConfiguration(blockchainRIDRaw: ByteArray, height: Long): ByteArray?
    fun findNextConfigurationHeight(blockchainRIDRaw: ByteArray, height: Long): Long?
}
