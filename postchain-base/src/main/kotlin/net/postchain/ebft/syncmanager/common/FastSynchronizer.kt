// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.syncmanager.common

import mu.KLogging
import net.postchain.base.BaseBlockHeader
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.core.*
import net.postchain.core.BlockHeader
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
 * Tuning parameters for FastSychronizer. All times are in ms.
 */
data class FastSyncParameters(var resurrectDrainedTime: Long = 10000,
                              var resurrectUnresponsiveTime: Long = 20000,
                              var parallelism: Int = 10,
                              /**
                               * How long to wait before deciding that we are the sole node
                               *
                               * Sane values:
                               *
                               * Replicas: Long.MAX_LONG
                               * Signers: 60000ms
                               * Tests with single node: 0 // skip fastsync
                               */
                              var discoveryTimeout: Long = 60000,
                              var pollPeersInterval: Long = 10000,
                              var jobTimeout: Long = 10000,
                              var loopInteval: Long = 100,
                              var processName: String = "")

/**
 * This class syncs blocks from its peers by requesting <parallelism> blocks
 * from random peers simultaneously.
 *
 * The peers respond to the requests using a BlockHeader immediately followed
 * by an UnfinishedBlock, but if they don't have the block, they respond with
 * their latest BlockHeader and the height that was requested. If they
 * don't have any blocks at all, they reply with a BlockHeader with empty header
 * and witness.
 *
 * Requests that times out: When a request to a peer has been outstanding for a
 * long time (fastSyncParams.jobTimeout), we must timeout and stop using that
 * peer, at least temporarily. Otherwise it will hold up the syncing process
 * every now and then when that peer has been (randomly) selected.
 *
 * When we start this process we have no knowledge of which peers we are connected
 * to. It's important to quickly get to know as many peers as possible, because the more
 * peers we have the more reliable we can determine if we're up-to-date or not.
 *
 * That's hidden by the CommunicationManager. Instead we rely on two discovery mechanisms:
 *
 * 1. Request blocks from random peers via communicationManager.sendToRandomPeer(), which returns
 * the peerId of the peer that the request was sent to.
 *
 * 2. Listen for messages from our peers. For example Status messages from peers that
 * are in normal sync mode are typically sent every ~1s, and block requests from peers in fastsync
 * mode are sent as often as they can, spread randomly across its peers, so the more peers it has,
 * the less often we receive requests from it. On the other hand if there are lots of peers we don't
 * need all peers to sync.
 *
 * These two methods should give us a pretty complete picture of the network within a few seconds.
 *
 * If this is the sole node, it will wait [params.discoveryTimeout] until it leaves fastsync and starts
 * building blocks on its own. This is not a problem in a real world scenario, since you can wait a minute
 * or so upon first start. But in tests, this can be really annoying. So tests that only runs a single node
 * should set [params.discoveryTimeout] to 0. For replicas, it should be set to MAX_LONG, because replicas
 * should stay in FastSync until shutdown.
 */
class FastSynchronizer(
        communicationManager: CommunicationManager<Message>,
        val blockDatabase: BlockDatabase,
        private val blockchainConfiguration: BlockchainConfiguration,
        blockQueries: BlockQueries,
        val params: FastSyncParameters
): Messaging(blockQueries, communicationManager) {
    private val jobs = TreeMap<Long, Job>()
    private val peerStatuses = PeerStatuses(params)

    // This is the communication mechanism from the async commitBlock callback to main loop
    private val finishedJobs = LinkedBlockingQueue<Job>()

    companion object: KLogging()

    var blockHeight: Long = blockQueries.getBestHeight().get()
        private set

    inner class Job(val height: Long, var peerId: XPeerID) {
        var header: BlockHeader? = null
        var witness: BlockWitness? = null
        var block: BlockDataWithWitness? = null
        var blockCommitting = false
        var success = false
        val startTime = System.currentTimeMillis()
        var hasRestartFailed = false
        override fun toString(): String {
            return "${this@FastSynchronizer.params.processName}-h${height}-${peerId.shortString()}"
        }
    }

    private val shutdown = AtomicBoolean(false)

    fun debug(message: String, e: Exception? = null) {
        logger.debug("${params.processName}: $message", e)
    }
    
    fun syncWhile(condition: () -> Boolean) {
        try {
            debug("Start fastsync")
            blockHeight = blockQueries.getBestHeight().get()
            debug("Best height $blockHeight")
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
                sleep(params.loopInteval)
            }
        } catch (e: Exception) {
            debug("Exception in syncWhile()", e)
        } finally {
            debug("Await commits")
            awaitCommits()
            jobs.clear()
            finishedJobs.clear()
            peerStatuses.clear()
            debug("Exit fastsync")
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
            debug("Processing done job $j")
            processDoneJob(j)
            j = finishedJobs.poll()
        }
    }

    private fun processDoneJob(j: Job, final: Boolean = false) {
        if (j.success) {
            // Add new job and remove old job
            if (!final) {
                startNextJob()
            }
            blockHeight++
            removeJob(j)
        } else {
            // If the job failed because the block is already in the database
            // then it means that fastsync started before all addBlock jobs
            // from normal sync were done. If this has happened, we
            // will increase the blockheight and consider this job done (but
            // not by us).
            val bestHeight = blockQueries.getBestHeight().get()
            if (bestHeight >= j.height) {
                debug("Add block failed for job ${j} because block already in db.")
                blockHeight++ // as if this block was successful.
                removeJob(j)
                return
            }

            debug("Invalid block ${j}. Blacklisting.")
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
            if (j.hasRestartFailed) {
                // These are jobs that couldn't be restarted because there
                // were no peers available at the time. Try again every
                // time, because there is virtually no cost in doing so.
                // It's just a check against a local datastructure.
                toRestart.add(j)
            } else if (j.block == null && j.startTime + params.jobTimeout < now) {
                // We have waited for response from j.peerId fo a long time.
                // Let's mark it unresponsive and restart the job.
                debug("Marking job ${j} unresponsive")
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
        debug("Discovery timeout: ${params.discoveryTimeout}")
        if (params.discoveryTimeout == 0L) {
            return false
        }
        val startTime = System.currentTimeMillis()
        while (!startNextJob()) {
            if (shutdown.get()) {
                return false
            }
            // We haven't got any connections yet, or we are sole node.
            // We don't know which. Retry after a while and fail after
            // timeout.
            if (System.currentTimeMillis()-startTime > params.discoveryTimeout) {
                // Assume we're sole node
                return false
            }
            sleep(10L)
        }
        return true
    }

    /**
     * This makes sure that we have <parallelism> jobs running
     * concurrently.
     */
    private fun refillJobs() {
        (jobs.size until params.parallelism).forEach {
            startNextJob()
        }
    }

    private fun restartJob(job: Job) {
        if (!startJob(job.height)) {
            // We had no peers available for this height, we'll have to try
            // again later. see processStaleJobs()
            job.hasRestartFailed = true
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
        val j = Job(height, peer)
        addJob(j)
        debug("Started job $j")
        return true
    }

    private fun removeJob(job: Job) {
        jobs.remove(job.height)
    }

    private fun addJob(job: Job) {
        peerStatuses.addPeer(job.peerId)
        val replaced = jobs.put(job.height, job)
        if (replaced == null) {
            debug("Added new job $job")
        } else {
            debug("Replaced job $replaced with $job")
        }
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
            debug("Peer for job $j drained at height -1")
            peerStatuses.drained(peerId, -1)
            restartJob(j)
            return
        }

        val h = blockchainConfiguration.decodeBlockHeader(header)
        val peerBestHeight = getHeight(h)

        if (peerBestHeight != j.height) {
            debug("Peer for $j drained at height $peerBestHeight")
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
            debug("Header for ${j} received")
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
            debug("Invalid header received for $j. Blacklisting.")
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
        debug("handleUnfinishedBlock received for $j")
        val expectedHeader = j.header
        if (j.block != null || peerId != j.peerId ||
                expectedHeader == null ||
                !(expectedHeader.rawData contentEquals header)) {
            // Got a block when we didn't expect one. Ignore it.
            debug("handleUnfinishedBlock didn't expect $j")
            return
        }
        // The witness has already been verified in handleBlockHeader().
        j.block = BlockDataWithWitness(h, txs, j.witness!!)

        for (job in jobs.values) {
            // The values are iterated in key-ascending order (see TreeMap)
            if (job.block == null) {
                // The next block to be committed hasn't arrived yet
                debug("handleUnfinishedBlock done. Next job, ${job}, to commit hasn't arrived yet.")
                return
            }
            if (!job.blockCommitting) {
                debug("handleUnfinishedBlock committing block for ${job}")
                job.blockCommitting = true
                commitBlock(job)
            }
        }
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
            debug("Exception committing block ${job}", it)
            finishedJobs.add(job)
        }
    }

    private fun processMessages() {
        for (packet in communicationManager.getPackets()) {
            val peerId = packet.first
            if (peerStatuses.isBlacklisted(peerId)) {
                continue
            }
            peerStatuses.addPeer(peerId)
            val message = packet.second
            try {
                when (message) {
                    is GetBlockAtHeight -> sendBlockAtHeight(peerId, message.height)
                    is GetBlockHeaderAndBlock -> sendBlockHeaderAndBlock(peerId, message.height, blockHeight)
                    is BlockHeaderMessage -> handleBlockHeader(peerId, message.header, message.witness, message.requestedHeight)
                    is UnfinishedBlock -> handleUnfinishedBlock(peerId, message.header, message.transactions)
                    is Status -> peerStatuses.statusReceived(peerId, message.height-1)
                    else -> debug("Unhandled type ${message} from peer $peerId")
                }
            } catch (e: Exception) {
                logger.info("Couldn't handle message $message from peer $peerId. Ignoring and continuing", e)
            }
        }
    }
}

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
     * The DRAINED state is reset to NOT_DRAINED whenever we receive a valid header for a
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
    private class KnownState(val params: FastSyncParameters) {
        private enum class State {
            BLACKLISTED, UNRESPONSIVE, NOT_DRAINED, DRAINED
        }
        private var state = State.NOT_DRAINED

        private var unresponsiveTime: Long = System.currentTimeMillis()
        private var drainedTime: Long = System.currentTimeMillis()
        private var drainedHeight: Long = -2

        fun isBlacklisted() = state == State.BLACKLISTED
        fun isUnresponsive() = state == State.UNRESPONSIVE
        private fun isDrained() = state == State.DRAINED
        fun isDrained(h: Long) = isDrained() && drainedHeight < h

        fun drained(height: Long) {
            state = State.DRAINED
            if (height > drainedHeight) {
                drainedHeight = height
                drainedTime = System.currentTimeMillis()
            }
        }
        fun received(height: Long) {
            if (state == State.DRAINED && height > drainedHeight) {
                state = State.NOT_DRAINED
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
            if (this.state != State.UNRESPONSIVE) {
                this.state = State.UNRESPONSIVE
                unresponsiveTime = System.currentTimeMillis()
            }
        }
        fun blacklist() {
            this.state = State.BLACKLISTED
        }
        fun resurrect(now: Long) {
            if (isDrained() && drainedTime + params.resurrectDrainedTime < now ||
                    isUnresponsive() && unresponsiveTime + params.resurrectUnresponsiveTime < now) {
                state = State.NOT_DRAINED
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
        status.unresponsive()
    }

    fun blacklist(peerId: XPeerID) {
        stateOf(peerId).blacklist()
    }

    private fun stateOf(peerId: XPeerID): KnownState {
        var knownState = statuses[peerId]
        if (knownState == null) {
            knownState = KnownState(params)
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