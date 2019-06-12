package net.postchain.ebft.rest.contract

import net.postchain.api.rest.contract.NodeStatusContract
import net.postchain.common.toHex
import net.postchain.ebft.NodeState
import net.postchain.ebft.NodeStatus

class EBFTstateNodeStatusContract(
        val state: NodeState,
        override val height: Long,
        override val serial: Long,
        override val round: Long,
        override val blockRid: String?,
        override val revolting: Boolean
): NodeStatusContract


fun NodeStatus.toNodeStatusContract() =
        EBFTstateNodeStatusContract(
                height = this.height,
                serial = this.serial,
                state = this.state,
                round = this.round,
                blockRid = this.blockRID?.toHex(),
                revolting = this.revolting
        )
