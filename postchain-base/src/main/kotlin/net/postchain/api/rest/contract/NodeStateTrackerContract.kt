package net.postchain.api.rest.contract

import net.postchain.common.toHex
import net.postchain.ebft.NodeState
import net.postchain.ebft.NodeStatus

interface NodeStateTrackerContract

class BlockHeight(val blockHeight: Long) : NodeStateTrackerContract

data class NodeStatusContract(
        val height: Long,
        val serial: Long,
        val state: NodeState,
        val round: Long,
        val blockRid: String?,
        val revolting: Boolean)

class MyStatus(val myStatus: NodeStatusContract) : NodeStateTrackerContract

class NodeStatuses(val statuses: Array<NodeStatusContract>) : NodeStateTrackerContract

fun NodeStatus.toNodeStatusContract() =
    NodeStatusContract(
            height = this.height,
            serial = this.serial,
            state = this.state,
            round = this.round,
            blockRid = this.blockRID?.toHex(),
            revolting = this.revolting
    )
