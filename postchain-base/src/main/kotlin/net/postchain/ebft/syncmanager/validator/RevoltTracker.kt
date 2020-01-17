package net.postchain.ebft.syncmanager.validator

import net.postchain.ebft.StatusManager
import java.util.*

class RevoltTracker(private val revoltTimeout: Int, private val statusManager: StatusManager) {
    var deadLine = newDeadLine()
    var prevHeight = statusManager.myStatus.height
    var prevRound = statusManager.myStatus.round

    /**
     * Set new deadline for the revolt tracker
     *
     * @return the time at which the deadline is passed
     */
    private fun newDeadLine(): Long {
        return Date().time + revoltTimeout
    }

    /**
     * Starts a revolt if certain conditions are met.
     */
    fun update() {
        val current = statusManager.myStatus
        if (current.height > prevHeight ||
                current.height == prevHeight && current.round > prevRound) {
            prevHeight = current.height
            prevRound = current.round
            deadLine = newDeadLine()
        } else if (Date().time > deadLine && !current.revolting) {
            this.statusManager.onStartRevolting()
        }
    }
}