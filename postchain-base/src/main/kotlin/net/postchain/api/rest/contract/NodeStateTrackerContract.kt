package net.postchain.api.rest.contract

interface NodeStateTrackerContract

class BlockHeight(val blockHeight: Long) : NodeStateTrackerContract

interface NodeStatusContract {
    val height: Long
    val serial: Long
    val round: Long
    val blockRid: String?
    val revolting: Boolean
}

class MyStatus(val myStatus: NodeStatusContract) : NodeStateTrackerContract

class NodeStatuses(val statuses: Array<NodeStatusContract>) : NodeStateTrackerContract
