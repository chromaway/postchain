// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.syncmanager.common

import mu.KLogging
import net.postchain.base.BaseBlockHeader
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.core.*
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.message.*
import net.postchain.network.CommunicationManager
import net.postchain.network.x.XPeerID
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap
import net.postchain.ebft.message.BlockHeader as BlockHeaderMessage

/**
 * This class syncs blocks from its peers by requesting <parallelism> blocks
 * from random peers simultaneously.
 *
 * The peers respond to the requests using a BlockHeader immediately followed
 * by an UnfinishedBlock, but if they don't have the block, they respond with
 * their latest BlockHeader and the height that was requested. If they
 * don't have any blocks at all, they don't reply.
 *
 * Requests that times out: When a request to a peer has been outstanding for a long time
 * we must timeout and stop using that peer, at least temporarily. Otherwise it will hold up the syncing
 * process every now and then when that peer has been randomly selected.
 */
class FastSynchronizer(
        communicationManager: CommunicationManager<Message>,
        val blockDatabase: BlockDatabase,
        private val blockchainConfiguration: BlockchainConfiguration,
        blockQueries: BlockQueries
): Messaging(blockQueries, communicationManager) {
    private val parallelism = 10
    private val discoveryTimeout = 60000
    private val pollPeersInterval = 10000
    private val jobTimeout = pollPeersInterval
    private val loopInterval = 100L // milliseconds

    private val jobs = TreeMap<Long, Job>()
    private val peerStatuses = PeerStatuses()

    // This is the communication mechanism from the async commitBlock callback to main loop
    private val finishedJobs = LinkedBlockingQueue<Job>()

    companion object: KLogging()

    var blockHeight: Long = blockQueries.getBestHeight().get()
        private set

    class Job(val height: Long, var peerId: XPeerID) {
        var header: BlockHeader? = null
        var witness: BlockWitness? = null
        var block: BlockDataWithWitness? = null
        var blockCommitting = false
        var success = false
        val startTime = System.currentTimeMillis()
    }

    private val shutdown = AtomicBoolean(false)

    fun syncWhile(condition: () -> Boolean) {
        try {
            logger.debug("Fastsync: Start fastsync")
            blockHeight = blockQueries.getBestHeight().get()
            logger.debug("Fastsync: Best height $blockHeight")
            if (!startFirstJob()) {
                // There are no nodes to sync from. We're
                // probably the sole live node
                return
            }
            while (!shutdown.get() && condition()) {
                refillJobs()
                processMessages()
                processDoneJobs()
                processStaleJobs()
                sleep(loopInterval)
            }
        } catch (e: Exception) {
            logger.debug("Exception in syncWhile()", e)
        } finally {
            logger.debug("Fastsync: Await commits")
            awaitCommits()
            jobs.clear()
            finishedJobs.clear()
            peerStatuses.clear()
            logger.debug("Fastsync: Exit fastsync")
        }
    }

    fun syncUntilShutdown() {
        syncWhile {true}
    }

    /**
     * Terminology:
     * current = our current view of the system
     * final = the actual effective configuration of the blockchain (that we may or may not have yet)
     *
     * This is called by a validator to make reasonably sure it's up-to-date with peers before
     * starting to build blocks.
     *
     * If no peer is responsive for X seconds, we'll assume we're the sole node and return.
     *
     * Note that if we have contact with all current nodes, it doesn't mean that we can trust that group,
     * because we don't know if this is the final configuration. Even if they are current signers, any/all
     * of those nodes could have turned rouge and got excluded from future signer lists. We'll have to
     * hope they'll provide us with block.
     *
     * When we have synced up to the final configuration, we *can* rely on the 2f+1 rule,
     * but we don't know when that happens. Any current signer list can be adversarial.
     *
     * All nodes are thus to be regarded as potentially adversarial/unreliable replicas.
     *
     * We consider ourselves up-to-date when we have drained roof(75% of all known peers).
     */
    fun syncUntilResponsiveNodesDrained() {
        syncWhile {
            val peerCount = peerStatuses.countAll()
            val drainedCount = peerStatuses.countDrained(blockHeight+1)
            if (peerCount < 4) {
                drainedCount == 0
            } else {
                // Integer version of "drainedCount.toDouble() / peerCount < 0.75"
                drainedCount * 4 < peerCount * 3
            }
        }
    }

    fun shutdown() {
        shutdown.set(true)
    }

    private fun awaitCommits() {
        val committingJobs = jobs.count { it.value.blockCommitting }
        for (i in (0 until committingJobs)) {
            val j = finishedJobs.take()
            processDoneJob(j, true)
        }
    }

    fun processDoneJobs() {
        var j = finishedJobs.poll()
        while (j != null) {
            processDoneJob(j)
            j = finishedJobs.poll()
        }
    }

    private fun processDoneJob(j: Job, final: Boolean = false) {
        if (j.success) {
            // Add new job and remove old job
            // Important to start new job before removing old, because
            // isAlmostFinished uses "jobs.size < 3" to decide whether almost
            // finished.
            // Don't ask drained nodes for it.
            if (!final) {
                startNextJob()
            }
            blockHeight++
            removeJob(j)
        } else {
            // Peer sent us an invalid block. Blacklist the peer and restart job
            peerStatuses.blacklist(j.peerId)
            if (!final) {
                restartJob(j)
            }
        }
    }

    fun processStaleJobs() {
        val now = System.currentTimeMillis()
        val toRestart = mutableListOf<Job>()
        for (j in jobs.values) {
            if (j.block == null && j.startTime + jobTimeout < now) {
                peerStatuses.unresponsive(j.peerId)
                toRestart.add(j)
            }
        }
        // Avoid ConcurrentModificationException by restartingJob after for loop
        toRestart.forEach {
            restartJob(it)
        }
    }

    private fun startFirstJob(): Boolean {
        val timeout = System.currentTimeMillis() + discoveryTimeout
        while (!startNextJob()) {
            if (shutdown.get()) {
                return false
            }
            // We haven't got any connections yet, or we are sole node.
            // We don't know which. Retry after a while and fail after
            // timeout.
            if (System.currentTimeMillis() > timeout) {
                // Assume we're sole node
                return false
            }
            sleep(10L)
        }
        return true
    }

    private fun refillJobs() {
        (jobs.size until parallelism).forEach {
            startNextJob()
        }
    }

    private fun restartJob(job: Job) {
        if (!startJob(job.height)) {
            removeJob(job)
        }
    }

    private fun startNextJob(): Boolean {
        return startJob(blockHeight + jobs.size + 1)
    }

    private fun startJob(height: Long): Boolean {
        val excludedPeers = peerStatuses.exclDrainedAndUnresponsive(height)
        var peer = communicationManager.sendToRandomPeer(GetBlockHeaderAndBlock(height), excludedPeers)
        if (peer == null) {
            return false
        }
        addJob(Job(height, peer))
        logger.debug("Fastsync: Started job for height $height, peer $peer")
        return true
    }

    private fun removeJob(job: Job) {
        jobs.remove(job.height)
    }

    private fun addJob(job: Job) {
        peerStatuses.addPeer(job.peerId)
        jobs.put(job.height, job)
    }

    private fun handleBlockHeader(peerId: XPeerID, header: ByteArray, witness: ByteArray, requestedHeight: Long) {
        val j = jobs[requestedHeight]
        if (j == null || j.header != null || peerId != j.peerId) {
            // Didn't expect header for this height or from this peer
            // We might want to blacklist peers that sends unsolicited headers;
            // They might be adversarial and try to get us to restart jobs
            // as much as they can. But hard to distinguish this from
            // legitimate glitches, for example that the peer has timed
            // out in earlier job but just now comes back with the response.
            return
        }

        if (header.size == 0 && witness.size == 0) {
            // The peer says it has no blocks, try another peer
            logger.debug { "Peer $peerId drained at height -1, requested height $requestedHeight" }
            peerStatuses.drained(peerId, -1)
            restartJob(j)
            return
        }

        val h = blockchainConfiguration.decodeBlockHeader(header)
        val peerBestHeight = getHeight(h)

        if (peerBestHeight != j.height) {
            logger.debug { "Peer $peerId drained at height $peerBestHeight, requested height $requestedHeight" }
            // The peer didn't have the block we wanted
            // Remember its height and try another peer
            peerStatuses.drained(peerId, peerBestHeight)
            restartJob(j)
            return
        }

        val w = blockchainConfiguration.decodeWitness(witness)
        if ((blockchainConfiguration as BaseBlockchainConfiguration).verifyBlockHeader(h, w)) {
            j.header = h
            j.witness = w
            logger.debug { "Header for height ${j.height} received from $peerId" }
            peerStatuses.headerReceived(peerId, peerBestHeight)
        } else {
            // There may be two resaons for verification failures.
            // 1. The peer is a scumbag, sending us invalid headers
            // 2. The header is from a configuration that we haven't activated yet.
            // In both cases we can blacklist the peer:
            //
            // 1. blacklisting scumbags is good
            // 2. The blockchain will restart before requestedHeight is added, so the
            // sync process will restart fresh with new configuration. Worst case is if
            // we download parallelism blocks before restarting.
            logger.debug { "Invalid header received from $peerId at height ${j.height}. Blacklisting." }
            peerStatuses.blacklist(peerId)
        }
    }

    private fun getHeight(header: BlockHeader): Long {
        // A bit ugly hack. Figure out something better. We shouldn't rely on specific
        // implementation here.
        // Our current implementation, BaseBlockHeader, includes the height, which
        // means that we can trust the height in the header because it's been
        // signed by a quorum of signers.
        // If another BlockHeader implementation is used, that doesn't include
        // the height, we'd have to rely on something else, for example
        // sending the height explicitly, but then we trust only that single
        // sender node to tell the truth.
        // For now we rely on the height being part of the header.
        if (header !is BaseBlockHeader) {
            throw ProgrammerMistake("Expected BaseBlockHeader")
        }
        return header.blockHeaderRec.getHeight()
    }

    private fun handleUnfinishedBlock(peerId: XPeerID, header: ByteArray, txs: List<ByteArray>) {
        val h = blockchainConfiguration.decodeBlockHeader(header)
        if (h !is BaseBlockHeader) {
            throw BadDataMistake(BadDataType.BAD_MESSAGE,"Expected BaseBlockHeader")
        }
        val height = getHeight(h)
        val j = jobs[height]?:return
        val expectedHeader = j.header
        if (j.block != null || peerId != j.peerId ||
                expectedHeader == null ||
                !(expectedHeader.rawData contentEquals header)) {
            // Got a block when we didn't expect one. Ignore it.
            return
        }
        // The witness has already been verified in handleBlockHeader().
        j.block = BlockDataWithWitness(h, txs, j.witness!!)

        for (job in jobs.values) {
            // The values are iterated in key-ascending order (see TreeMap)
            if (job.block == null) {
                // The next block to be committed hasn't arrived yet
                return
            }
            if (!job.blockCommitting) {
                job.blockCommitting = true
                commitBlock(job)
            }
        }
    }

    private fun handleStatus(peerId: XPeerID, height: Long) {
        peerStatuses.statusReceived(peerId, height)
    }

    private fun commitBlock(job: Job) {
        val p = blockDatabase.addBlock(job.block!!)
        p.success {_ ->
            job.success = true
            finishedJobs.add(job)
        }
        p.fail {
            // We got an invalid block from peer. Let's blacklist this
            // peer and try another peer
            finishedJobs.add(job)
        }
    }

    private fun processMessages() {
        for (packet in communicationManager.getPackets()) {
            val xPeerId = packet.first
            if (peerStatuses.isBlacklisted(xPeerId)) {
                continue
            }
            val message = packet.second
            try {
                when (message) {
                    is GetBlockAtHeight -> sendBlockAtHeight(xPeerId, message.height)
                    is GetBlockHeaderAndBlock -> sendBlockHeaderAndBlock(xPeerId, message.height, blockHeight)
                    is BlockHeaderMessage -> handleBlockHeader(xPeerId, message.header, message.witness, message.requestedHeight)
                    is UnfinishedBlock -> handleUnfinishedBlock(xPeerId, message.header, message.transactions)
                    is Status -> handleStatus(xPeerId, message.height-1)
                    else -> logger.debug("Unhandled type ${message} from peer $xPeerId")
                }
            } catch (e: Exception) {
                logger.info("Couldn't handle message $message from peer $xPeerId. Ignoring and continuing", e)
            }
        }
    }
}

/**
 * Keeps track of peer's statuses. The currently trackeed statuses are
 *
 * Blacklisted: We have received invalid data from the peer
 * Unresponsive: We haven't received a timely response from the peer
 * NotDrained: This class doesn't have any useful information about the peer
 * Drained: The peer's tip is reached.
 */
class PeerStatuses: KLogging() {
    companion object {
        const val RESURRECTDRAINEDTIME = 10000
        const val RESURRECTUNRESPONSIVE = 20000
    }

    private class KnownState() {
        private enum class State {
            BLACKLISTED, UNRESPONSIVE, NOT_DRAINED, DRAINED
        }
        private var time: Long = System.currentTimeMillis()
            private set
        private var receivedHeight: Long = -1
        private var drainedHeight: Long = -2
        private var state = State.NOT_DRAINED

        fun isBlacklisted() = state == State.BLACKLISTED
        fun isUnresponsive() = state == State.UNRESPONSIVE
        private fun isDrained() = state == State.DRAINED
        fun isDrained(h: Long) = isDrained() && drainedHeight < h
        fun drained(height: Long) {
            state = State.DRAINED
            if (height > drainedHeight) {
                drainedHeight = height
            }
        }
        fun received(height: Long) {
            if (state == State.DRAINED && height > drainedHeight) {
                state = State.NOT_DRAINED
            }
            if (receivedHeight < height) {
                receivedHeight = height
            }
        }
        fun statusReceived(height: Long) {
            // We take a Status message as an indication that
            // there might be more blocks to fetch now. But
            // we won't resurrect unresponsive peers.
            if (state == State.DRAINED && height > drainedHeight) {
                state = State.NOT_DRAINED
            }
        }
        fun unresponsive() {
            this.state = State.UNRESPONSIVE
        }
        fun blacklist() {
            this.state = State.BLACKLISTED
        }
        fun resurrect(now: Long) {
            if (isDrained() && time + RESURRECTDRAINEDTIME < now ||
                    isUnresponsive() && time + RESURRECTUNRESPONSIVE < now) {
                state = State.NOT_DRAINED
                time = now
            }
        }
    }
    private val statuses = HashMap<XPeerID, KnownState>()

    private fun resurrectDrainedAndUnresponsivePeers() {
        val now = System.currentTimeMillis()
        // Resurrect drained and unresponsive nodes
        statuses.forEach {
            it.value.resurrect(now)
        }
    }

    fun exclDrainedAndUnresponsive(height: Long): Set<XPeerID> {
        resurrectDrainedAndUnresponsivePeers()
        return statuses.filterValues { it.isDrained(height) || it.isUnresponsive() || it.isBlacklisted() }.keys
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
        status.received(height)
    }

    fun statusReceived(peerId: XPeerID, height: Long) {
        val status = stateOf(peerId)
        if (status.isBlacklisted()) {
            return
        }
        status.statusReceived(height)
    }

    fun unresponsive(peerId: XPeerID) {
        val status = stateOf(peerId)
        if (status.isBlacklisted()) {
            return
        }
        logger.debug("Fastsync: marking unresponsive: $peerId")
        status.unresponsive()
    }

    fun blacklist(peerId: XPeerID) {
        stateOf(peerId).blacklist()
    }

    private fun stateOf(peerId: XPeerID): KnownState {
        var knownState = statuses[peerId]
        if (knownState == null) {
            knownState = KnownState()
            statuses[peerId] = knownState
        }
        return knownState
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

    fun countDrained(height: Long): Int {
        return statuses.count { it.value.isDrained(height) }
    }

    fun countAll(): Int {
        return statuses.size
    }

    fun clear() {
        statuses.clear()
    }
}