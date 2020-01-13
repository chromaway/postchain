package net.postchain.ebft.syncmanager

import net.postchain.ebft.StatusManager
import net.postchain.ebft.message.Message
import net.postchain.ebft.message.Status
import net.postchain.network.CommunicationManager
import java.util.*

class StatusSender(
        private val maxStatusInterval: Int,
        private val statusManager: StatusManager,
        private val communicationManager: CommunicationManager<Message>
) {
    var lastSerial: Long = -1
    var lastSentTime: Long = Date(0L).time

    // Sends a status message to all peers when my status has changed or
    // after a timeout period.
    fun update() {
        val myStatus = statusManager.myStatus
        val isNewState = myStatus.serial > this.lastSerial
        val timeoutExpired = System.currentTimeMillis() - this.lastSentTime > this.maxStatusInterval
        if (isNewState || timeoutExpired) {
            this.lastSentTime = Date().time
            this.lastSerial = myStatus.serial
            val statusMessage = Status(myStatus.blockRID, myStatus.height,
                    myStatus.revolting, myStatus.round, myStatus.serial,
                    myStatus.state.ordinal)
            communicationManager.broadcastPacket(statusMessage)
        }
    }
}