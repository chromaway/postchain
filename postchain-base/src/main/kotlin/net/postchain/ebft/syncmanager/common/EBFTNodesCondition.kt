package net.postchain.ebft.syncmanager.common

import net.postchain.ebft.NodeStatus
import net.postchain.getBFTRequiredSignatureCount

class EBFTNodesCondition(
        val nodeStatuses: Array<NodeStatus>,
        val condition: (NodeStatus) -> Boolean
) {

    fun satisfied(): Boolean {
        val passed = nodeStatuses.filter { status -> condition(status) }
        val quorum = getBFTRequiredSignatureCount(nodeStatuses.size)
        return passed.size >= quorum
    }
}