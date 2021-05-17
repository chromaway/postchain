package net.postchain.ebft.syncmanager.common

import mu.KLogging
import net.postchain.network.x.XPeerID

/**
 * Keeps track of peer's statuses. The currently trackeed statuses are
 *
 * Blacklisted: We have received invalid data from the peer, or it's otherwise misbehaving
 * Unresponsive: We haven't received a timely response from the peer
 * NotDrained: This class doesn't have any useful information about the peer
 * Drained: The peer's tip is reached.
 */
class PeerStatuses(val params: FastSyncParameters): KLogging() {

    /**
     * Keeps notes on a single peer. Some rules:
     *
     * When a peer has been marked DRAINED or UNRESPONSIVE for a certain
     * amount of time ([params.resurrectDrainedTime] and
     * [params.resurrectUnresponsiveTime] resp.) it will be given
     * a new chance to serve us blocks. Otherwise we might run out of
     * peers to sync from over time.
     *
     * Peers that are marked BLACKLISTED, should never be given another chance
     * because they have been proven to provide bad data (deliberately or not).
     *
     * The DRAINED state is reset to SYNCABLE whenever we receive a valid header for a
     * height higher than the height at which it was drained or when we
     * receive a Status message (which is sent regurarly from peers in normal
     * sync mode).
     *
     * We use Status messages as indication that there are headers
     * available at that Status' height-1 (The height in the Status
     * message indicates the height that they're working on, ie their committed
     * height + 1). They also serve as a discovery mechanism, in which we become
     * aware of our neiborhood.
     */
    class KnownState(val params: FastSyncParameters): KLogging() {
        private enum class State {
            BLACKLISTED, UNRESPONSIVE, SYNCABLE, DRAINED
        }

        private var state = State.SYNCABLE

        /**
         * [maybeLegacy] and [confirmedModern] are transitional and should be
         * removed once most nodes have upgraded, because then
         * nodes will be able to sync from modern nodes and we no longer
         * need to be able to sync from old nodes.
         */
        private var maybeLegacy = false
        private var confirmedModern = false
        private var unresponsiveTime: Long = System.currentTimeMillis()
        private var drainedTime: Long = System.currentTimeMillis()
        private var drainedHeight: Long = -1
        private var errorCount = 0
        private var timeOfLastError: Long = 0

        fun isBlacklisted() = isBlacklisted(System.currentTimeMillis())
        fun isBlacklisted(now: Long): Boolean {
            if (state == State.BLACKLISTED && (now > timeOfLastError + params.blacklistingTimeoutMs)) {
                // Alex suggested that peers should be given new chances often
                logger.debug("Peer timed out of blacklist")
                this.state = State.SYNCABLE
                this.timeOfLastError = 0
                this.errorCount = 0
            }

            return state == State.BLACKLISTED
        }
        fun isUnresponsive() = state == State.UNRESPONSIVE
        fun isMaybeLegacy() = !confirmedModern && maybeLegacy
        fun isConfirmedModern() = confirmedModern
        fun isSyncable(h: Long) = state == State.SYNCABLE || state == State.DRAINED && drainedHeight >= h

        fun drained(height: Long) {
            state = State.DRAINED
            drainedTime = System.currentTimeMillis()
            if (height > drainedHeight) {
                drainedHeight = height
            }
        }
        fun headerReceived(height: Long) {
            if (state == State.DRAINED && height > drainedHeight) {
                state = State.SYNCABLE
            }
        }
        fun statusReceived(height: Long) {
            // We take a Status message as an indication that
            // there might be more blocks to fetch now. But
            // we won't resurrect unresponsive peers.
            if (state == State.DRAINED && height > drainedHeight) {
                state = State.SYNCABLE
            }
        }
        fun unresponsive(desc: String, now: Long) {
            if (this.state != State.UNRESPONSIVE) {
                this.state = State.UNRESPONSIVE
                unresponsiveTime = now
                logger.debug("Peer is UNRESPONSIVE: $desc")
            }
        }
        fun maybeLegacy(isLegacy: Boolean) {
            if (!this.confirmedModern) {
                this.maybeLegacy = isLegacy
            }
        }
        fun confirmedModern() {
            this.confirmedModern = true
            this.maybeLegacy = false
        }

        fun blacklist(desc: String, now: Long) {
            if (this.state != State.BLACKLISTED) {
                errorCount++
                if (errorCount >= params.maxErrorsBeforeBlacklisting) {
                    logger.warn("Blacklisting peer: $desc")
                    this.state = State.BLACKLISTED
                    this.timeOfLastError = now
                } else {
                    if (logger.isTraceEnabled) {
                        logger.trace("Not blacklisting peer: $desc")
                    }
                    this.timeOfLastError = now
                }
            }
        }
        fun resurrect(now: Long) {
            if (state == State.DRAINED && drainedTime + params.resurrectDrainedTime < now ||
                    isUnresponsive() && unresponsiveTime + params.resurrectUnresponsiveTime < now) {
                state = State.SYNCABLE
            }
        }
    }
    private val statuses = HashMap<XPeerID, KnownState>()

    private fun resurrectDrainedAndUnresponsivePeers() {
        val now = System.currentTimeMillis()
        statuses.forEach {
            it.value.resurrect(now)
        }
    }

    fun exclNonSyncable(height: Long): Set<XPeerID> {
        resurrectDrainedAndUnresponsivePeers()
        return statuses.filterValues { !it.isSyncable(height) || it.isMaybeLegacy() }.keys
    }

    fun getLegacyPeers(height: Long): Set<XPeerID> {
        return statuses.filterValues { it.isMaybeLegacy() && it.isSyncable(height) }.keys
    }

    fun drained(peerId: XPeerID, height: Long) {
        val status = stateOf(peerId)
        if (status.isBlacklisted()) {
            return
        }
        status.drained(height)
    }

    fun headerReceived(peerId: XPeerID, height: Long) {
        val status = stateOf(peerId)
        if (status.isBlacklisted()) {
            return
        }
        status.headerReceived(height)
    }

    fun statusReceived(peerId: XPeerID, height: Long) {
        val status = stateOf(peerId)
        if (status.isBlacklisted()) {
            return
        }
        status.statusReceived(height)
    }

    /**
     * @param peerId is the peer that's not responding
     * @param desc is the text we will log, surrounding the circumstances.
     *             (This could be caused by a bug, if so it has to be traced)
     */
    fun unresponsive(peerId: XPeerID, desc: String) {
        val status = stateOf(peerId)
        if (status.isBlacklisted()) {
            return
        }
        status.unresponsive(desc, System.currentTimeMillis())
    }

    fun setMaybeLegacy(peerId: XPeerID, isLegacy: Boolean) {
        val status = stateOf(peerId)
        if (status.isBlacklisted()) {
            return
        }
        status.maybeLegacy(isLegacy)
    }

    fun isMaybeLegacy(peerId: XPeerID): Boolean {
        return stateOf(peerId).isMaybeLegacy()
    }
    fun isConfirmedModern(peerId: XPeerID): Boolean {
        return stateOf(peerId).isConfirmedModern()
    }
    fun confirmModern(peerId: XPeerID) {
        stateOf(peerId).confirmedModern()
    }

    /**
     * Might blacklist this peer depending on number of failures.
     *
     * @param peerId is the peer that's behaving badly
     * @param desc is the text we will log, surrounding the circumstances.
     *             (This could be caused by a bug, if so it has to be traced)
     */
    fun maybeBlacklist(peerId: XPeerID, desc: String) {
        stateOf(peerId).blacklist(desc, System.currentTimeMillis())
    }

    private fun stateOf(peerId: XPeerID): KnownState {
        return statuses.computeIfAbsent(peerId) { KnownState(params) }
    }

    /**
     * Adds the peer if it doesn't exist. Do nothing if it exists.
     */
    fun addPeer(peerId: XPeerID) {
        stateOf(peerId)
    }

    fun isBlacklisted(xPeerId: XPeerID): Boolean {
        return stateOf(xPeerId).isBlacklisted()
    }

    fun getSyncable(height: Long): Set<XPeerID> {
        return statuses.filterValues { it.isSyncable(height) }.map {it.key}.toSet()
    }

    fun clear() {
        statuses.clear()
    }
}
