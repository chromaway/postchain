package net.postchain.ebft.syncmanager

import net.postchain.ebft.NodeStatus

class NodeStateTracker {
    var nodeStatuses: Array<NodeStatus>? = null
    var myStatus: NodeStatus? = null
    var blockHeight: Long = -1
}
