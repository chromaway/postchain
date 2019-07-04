package net.postchain.ebft.syncmanager.fastsync

import net.postchain.core.BlockDataWithWitness

data class IncomingBlock(val block: BlockDataWithWitness, val height: Long) : Comparable<IncomingBlock> {
    override fun compareTo(other: IncomingBlock) = when {
        height < other.height -> -1
        height > other.height -> 1
        else -> 0
    }
}
