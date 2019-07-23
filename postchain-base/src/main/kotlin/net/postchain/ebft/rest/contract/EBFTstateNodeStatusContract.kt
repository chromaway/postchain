package net.postchain.ebft.rest.contract

import net.postchain.api.rest.json.JsonFactory
import net.postchain.common.toHex
import net.postchain.ebft.NodeState
import net.postchain.ebft.NodeStatus

class EBFTstateNodeStatusContract(
        val state: NodeState,
        val height: Long,
        val serial: Long,
        val round: Long,
        val blockRid: String?,
        val revolting: Boolean
)

fun NodeStatus.serialize(): String {
    val gson = JsonFactory.makeJson()
    val contract = EBFTstateNodeStatusContract(
            height = this.height,
            serial = this.serial,
            state = this.state,
            round = this.round,
            blockRid = this.blockRID?.toHex(),
            revolting = this.revolting
    )
    return gson.toJson(contract)
}
