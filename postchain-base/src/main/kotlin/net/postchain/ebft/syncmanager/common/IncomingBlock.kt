package net.postchain.ebft.syncmanager.common

import net.postchain.core.BlockDataWithWitness

data class IncomingBlock(val block: BlockDataWithWitness, val height: Long) : Comparable<IncomingBlock> {
    override fun compareTo(other: IncomingBlock) = height.compareTo(other.height)
}
