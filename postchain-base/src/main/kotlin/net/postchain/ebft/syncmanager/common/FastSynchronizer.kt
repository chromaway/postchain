// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.syncmanager.common

import mu.KLogging
import net.postchain.base.BaseBlockHeader
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.core.*
import net.postchain.debug.BlockTrace
import net.postchain.ebft.BDBAbortException
import net.postchain.ebft.BlockDatabase
import net.postchain.ebft.CompletionPromise
import net.postchain.ebft.message.*
import net.postchain.ebft.worker.WorkerContext
import net.postchain.network.x.XPeerID
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import net.postchain.ebft.message.BlockData as MessageBlockData
import net.postchain.ebft.message.BlockHeader as BlockHeaderMessage


/**
 * Tuning parameters for FastSychronizer. All times are in ms.
 */
data class FastSyncParameters(var resurrectDrainedTime: Long = 10000,
                              var resurrectUnresponsiveTime: Long = 20000,
                              /**
                               * For tiny blocks it might make sense to increase parallelism to, eg 100,
                               * to increase throughput by ~6x (as experienced through experiments),
                               * but for non-trivial blockchains, this will require substantial amounts
                               * of memory, worst case about parallelism*blocksize.
                               *
                               * There seems to be a sweet-spot throughput-wise at parallelism=120,
                               * but it can come at great memory cost. We set this to 10
                               * to be safe.
                               *
                               * Ultimately, this should be a configuration setting.
                               */
                              var parallelism: Int = 10,
                              /**
                               * Don't exit fastsync for at least this amount of time (ms).
                               * This gives the connection manager some time to accumulate
                               * connections so that the random peer selection has more
                               * peers to chose from, to avoid exiting fastsync
                               * prematurely because one peer is connected quicker, giving
                               * us the impression that there is only one reachable node.
                               *
                               * Example: I'm A(height=-1), and B(-1),C(-1),D(0) are peers. When entering FastSync
                               * we're only connected to B.
                               *
                               * * Send a GetBlockHeaderAndBlock(0) to B
                               * * B replies with empty block header and we mark it as drained(-1).
                               * * We conclude that we have drained all peers at -1 and exit fastsync
                               * * C and D connections are established.
                               *
                               * We have exited fastsync before we had a chance to sync from C and D
                               *
                               * Sane values:
                               * Replicas: not used
                               * Signers: 60000ms
                               * Tests with single node: 0
                               * Tests with multiple nodes: 1000
                               */
                              var exitDelay: Long = 60000,
                              var pollPeersInterval: Long = 10000,
                              var jobTimeout: Long = 10000,
                              var loopInterval: Long = 100,
                              var mustSyncUntilHeight: Long = -1,
                              var maxErrorsBeforeBlacklisting: Int = 10,
                              /**
                               * 10 minutes in milliseconds
                               */
                              var blacklistingTimeoutMs: Long = 10 * 60 * 1000)

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
 * We only use random known peers (from the peerCommConfiguration) to sync from.
 *
 * If there are no live peers, it will wait [params.exitDelay] until it leaves fastsync and starts
 * trying to build blocks on its own. This is not a problem in a real world scenario, since you can wait a minute
 * or so upon first start. But in tests, this can be really annoying. So tests that only runs a single node
 * should set [params.exitDelay] to 0.
 */
class FastSynchronizer(private val workerContext: WorkerContext,
                       val blockDatabase: BlockDatabase,
                       val params: FastSyncParameters
) : Messaging(workerContext.engine.getBlockQueries(), workerContext.communicationManager) {
    private val blockchainConfiguration = workerContext.engine.getConfiguration()
    private val configuredPeers = workerContext.peerCommConfiguration.networkNodes.getPeerIds()
    private val jobs = TreeMap<Long, Job>()
    private val peerStatuses = PeerStatuses(params)
    private var lastJob: Job? = null

    // this is used to track pending asynchronous BlockDatabase.addBlock tasks to make sure failure to commit propagates properly
    private var addBlockCompletionPromise: CompletionPromise? = null

    // This is the communication mechanism from the async commitBlock callback to main loop
    private val finishedJobs = LinkedBlockingQueue<Job>()

    companion object : KLogging()

    var blockHeight: Long = workerContext.engine.getBlockQueries().getBestHeight().get()
        private set

    inner class Job(val height: Long, var peerId: XPeerID) {
        var header: BlockHeader? = null
        var witness: BlockWitness? = null
        var block: BlockDataWithWitness? = null
        var blockCommitting = false
        var addBlockException: Exception? = null
        val startTime = System.currentTimeMillis()
        var hasRestartFailed = false
        override fun toString(): String {
            return "${this@FastSynchronizer.workerContext.processName}-h${height}-${peerId.shortString()}"
        }
    }

    private val shutdown = AtomicBoolean(false)

    fun syncUntil(exitCondition: () -> Boolean) {
        try {
            blockHeight = blockQueries.getBestHeight().get()
            syncDebug("Start", blockHeight)
            while (!shutdown.get() && !exitCondition()) {
                refillJobs()
                processMessages()
                processDoneJobs()
                processStaleJobs()
                sleep(params.loopInterval)
            }
        } catch (e: BadDataMistake) {
            error("Fatal error, shutting down blockchain for safety reasons. Needs manual investigation.", e)
            throw e
        } catch (e: Exception) {
            syncDebug("Exception", e)
        } finally {
            syncDebug("Await commits", blockHeight)
            awaitCommits()
            jobs.clear()
            finishedJobs.clear()
            peerStatuses.clear()
            syncDebug("Exit fastsync", blockHeight)
        }
    }

    fun syncUntilShutdown() {
        syncUntil { false }
    }

    /**
     * Terminology:
     * current = our current view of the system
     * final = the actual effective configuration of the blockchain (that we may or may not have yet)
     * drained(h) = peer that we have signalled that it hasn't any blocks *after* height h, ie h is its tip.
     * syncable(h) = responsive peer that isn't (yet) drained(h)
     *
     * This is called by a validator to make reasonably sure it's up-to-date with peers before
     * starting to build blocks.
     *
     * If no peer is responsive for X seconds, we'll assume we're the sole live node and return.
     *
     * Note that if we have contact with all current signers, it doesn't mean that we can trust that group,
     * because we don't know if this is the final configuration. Even if they are current signers, any/all
     * of those nodes could have turned rouge and got excluded from future signer lists. We'll have to
     * hope they'll provide us with correct blocks.
     *
     * When we have synced up to the final configuration, we *can* rely on the 2f+1 rule,
     * but we won't be aware of that when it happens. Any current signer list can be adversarial.
     *
     * All nodes are thus to be regarded as potentially adversarial/unreliable replicas.
     *
     * We consider ourselves up-to-date when
     * (a) at least [exitDelay] ms has passed since start and
     * (b) we have no syncable peers at our own height.
     *
     * The time requirement (a) is to allow for connections to be established to as many peers as
     * possible (within reasonable limit) before considering (b). Otherwise (b) might be trivially true
     * if we only had time to connect to a single or very few nodes.
     */
    fun syncUntilResponsiveNodesDrained() {
        val timeout = System.currentTimeMillis() + params.exitDelay
        trace("exitDelay: ${params.exitDelay}")
        syncUntil {
            val syncableCount = peerStatuses.getSyncable(blockHeight + 1).intersect(configuredPeers).size

            // Keep syncing until this becomes true, i.e. to exit we must have:
            timeout < System.currentTimeMillis()                 // 1. must have timeout
                    && syncableCount == 0                        // 2. must have no syncable nodes
                    && blockHeight >= params.mustSyncUntilHeight // 3. must BC height above the minimum specified height
        }
    }

    fun shutdown() {
        shutdown.set(true)
    }

    private fun awaitCommits() {
        // Check also hasRestartFailed to avoid getting stuck in awaitCommits(). If we don't check it
        // AND
        //
        // * j.peerId was blacklisted in previous invocation of processDoneJob AND
        // * restartJob(j) doesn't find a peer to send to, and thus doesn't remove it.
        //
        // then we will count this job as currently committing and wait for more
        // committing blocks as are actually committing.
        val committingJobs = jobs.count { it.value.blockCommitting && !it.value.hasRestartFailed }
        for (i in (0 until committingJobs)) {
            val j = finishedJobs.take()
            processDoneJob(j, true)
        }
    }

    private fun processDoneJobs() {
        var j = finishedJobs.poll()
        while (j != null) {
            processDoneJob(j)
            j = finishedJobs.poll()
        }
    }

    /**
     * Depending on the result of the job we take different actions.
     */
    private fun processDoneJob(j: Job, final: Boolean = false) {
        val exception = j.addBlockException
        if (exception == null) {
            doneDebug("Job $j done")
            // Add new job and remove old job
            if (!final) {
                startNextJob()
            }
            blockHeight++
            removeJob(j)
            // Keep track of last block's job, in case of a BadDataType.PREV_BLOCK_MISMATCH on next job
            // Discard bulky data we don't need
            j.block = null
            j.witness = null
            lastJob = j
        } else {
            if (exception is PmEngineIsAlreadyClosed) {
                doneTrace("Add block failed for job $j because Db Engine is already closed.")
                removeJob(j)
                return
            }

            if (exception is BDBAbortException) {
                // the problem happened because of a previous block, clean up this
                // job and resubmit
                j.blockCommitting = false
                j.addBlockException = null
                commitJobsAsNecessary(null) // TODO: bTrace?
                return
            }

            if (exception is BadDataMistake && exception.type == BadDataType.PREV_BLOCK_MISMATCH) {
                // If we can't connect block, then either
                // previous block is bad or this block is bad. Unfortunately,
                // we can't know which. Ideally, we'd like to take different actions:
                // If this block is bad -> blacklist j.peer
                // If previous block is bad -> Panic shutdown blockchain
                //
                // We take the cautious approach and always shutdown the
                // blockchain. We also log this block's job and last block's job
                doneInfo("Previous block mismatch. " +
                        "Previous block ${lastJob?.header?.blockRID} received from ${lastJob?.peerId}, " +
                        "This block ${j.header?.blockRID} received from ${j.peerId}.")
                throw exception
            }

            // If the job failed because the block is already in the database
            // then it means that fastsync started before all addBlock jobs
            // from normal sync were done. If this has happened, we
            // will increase the blockheight and consider this job done (but
            // not by us).
            val bestHeight = blockQueries.getBestHeight().get()
            if (bestHeight >= j.height) {
                doneTrace("Add block failed for job $j because block already in db.")
                blockHeight++ // as if this block was successful.
                removeJob(j)
                return
            }

            val errMsg = "Invalid block $j. Blacklisting peer ${j.peerId}: ${exception.message}"
            doneError(errMsg)
            // Peer sent us an invalid block. Blacklist the peer and restart job
            peerStatuses.maybeBlacklist(j.peerId, errMsg)
            if (final) {
                removeJob(j)
            } else {
                restartJob(j)
            }
        }
    }

    fun processStaleJobs() {
        val now = System.currentTimeMillis()
        val toRestart = mutableListOf<Job>()
        // Keep track of peers that we mark legacy. Otherwise, if same peer appears in
        // multiple timed out jobs, we will
        // 1) Mark it as maybeLegacy on first appearance
        // 2) Mark it as unresponsive and not maybeLegacy on second appearance
        // The result is that we won't send legacy request to that peer, since it's marked
        // unresponsive.
        val legacyPeers = mutableSetOf<XPeerID>()
        for (j in jobs.values) {
            if (j.hasRestartFailed) {
                if (j.startTime + params.jobTimeout < now) {
                    peerStatuses.unresponsive(j.peerId, "Synch: Marking peer for restarted job ${j} unresponsive")
                }
                // These are jobs that couldn't be restarted because there
                // were no peers available at the time. Try again every
                // time, because there is virtually no cost in doing so.
                // It's just a check against some local datastructures.
                toRestart.add(j)
            } else if (j.block == null && j.startTime + params.jobTimeout < now) {
                // We have waited for response from j.peerId for a long time.
                // This might be because it's a legacy node and thus doesn't respond to
                // GetBlockHeaderAndBlock messages or because it's just unresponsive
                if (peerStatuses.isConfirmedModern(j.peerId)) {
                    peerStatuses.unresponsive(j.peerId, "Synch: Marking modern peer for job ${j} unresponsive")
                } else if (!legacyPeers.contains(j.peerId) && peerStatuses.isMaybeLegacy(j.peerId)) {
                    // Peer is marked as legacy, but still appears unresponsive.
                    // This probably wasn't a legacy node, but simply an unresponsive one.
                    // It *could* still be a legacy node, but we give it another chance to
                    // prove itself a modern node after the timeout
                    peerStatuses.setMaybeLegacy(j.peerId, false)
                    peerStatuses.unresponsive(j.peerId, "Synch: Marking potentially legacy peer for job ${j} unresponsive")
                } else {
                    // Let's assume this is a legacy node and use GetCompleteBlock for the
                    // next try.
                    // If that try is unresponsive too, we'll mark it as unresponsive
                    peerStatuses.setMaybeLegacy(j.peerId, true)
                    legacyPeers.add(j.peerId)
                }
                toRestart.add(j)
            }
        }
        // Avoid ConcurrentModificationException by restartingJob after for loop
        toRestart.forEach {
            restartJob(it)
        }
    }

    /**
     * This makes sure that we have <parallelism> number of jobs running
     * concurrently.
     */
    private fun refillJobs() {
        (jobs.size until params.parallelism).forEach {
            if (!startNextJob()) {
                // There are no peers to talk to
                return
            }
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

    private fun sendLegacyRequest(height: Long): XPeerID? {
        val peers = peerStatuses.getLegacyPeers(height).intersect(configuredPeers)
        if (peers.isEmpty()) return null
        return communicationManager.sendToRandomPeer(GetBlockAtHeight(height), peers)
    }

    private fun sendRequest(height: Long): XPeerID? {
        val excludedPeers = peerStatuses.exclNonSyncable(height)
        val peers = configuredPeers.minus(excludedPeers)
        if (peers.isEmpty()) return null
        return communicationManager.sendToRandomPeer(GetBlockHeaderAndBlock(height), peers)
    }

    private fun startJob(height: Long): Boolean {
        var peer = sendRequest(height)
        if (peer == null) {
            // There were no modern nodes to sync from. Let's try with a legacy node instead
            peer = sendLegacyRequest(height)
            if (peer == null) {
                // there were no peers at all to sync from. give up.
                return false
            }
        }
        val j = Job(height, peer)
        addJob(j)
        startJobTrace("Started job $j")
        return true
    }

    private fun removeJob(job: Job) {
        jobs.remove(job.height)
    }

    private fun addJob(job: Job) {
        peerStatuses.addPeer(job.peerId) // Only adds if peer doesn't exist
        val replaced = jobs.put(job.height, job)
        if (replaced == null) {
            addTrace("Added new job $job")
        } else {
            addTrace("Replaced job $replaced with $job")
        }
    }

    private fun debugJobString(j: Job?, requestedHeight: Long, peerId: XPeerID): String {
        var out = ", Received: height: $requestedHeight , peerId: $peerId"
        if (j != null) {
            out += ", Requested (job): $j"
        }
        return out
    }

    private fun handleBlockHeader(peerId: XPeerID, header: ByteArray, witness: ByteArray, requestedHeight: Long): Boolean {
        val j = jobs[requestedHeight]

        // Didn't expect header for this height or from this peer
        // We might want to blacklist peers that sends unsolicited headers;
        // They might be adversarial and try to get us to restart jobs
        // as much as they can. But hard to distinguish this from
        // legitimate glitches, for example that the peer has timed
        // out in earlier job but just now comes back with the response.
        if (j == null) {
            var dbg = debugJobString(j, requestedHeight, peerId)
            peerStatuses.maybeBlacklist(peerId, "Synch: Why do we receive a header for a block height not in our job list? $dbg")
            return false
        }
        if (peerId != j.peerId) {
            var dbg = debugJobString(j, requestedHeight, peerId)
            peerStatuses.maybeBlacklist(peerId, "Synch: Why do we receive a header from a peer when we didn't ask this peer? $dbg")
            return false
        }
        if (j.header != null) {
            var dbg = debugJobString(j, requestedHeight, peerId)
            peerStatuses.maybeBlacklist(peerId, "Synch: Why do we receive a header when we already have the header? $dbg")
            return false
        }

        if (header.size == 0 && witness.size == 0) {
            // The peer says it has no blocks, try another peer
            headerTrace("Peer for job $j drained", -1L)
            peerStatuses.drained(peerId, -1)
            restartJob(j)
            return false
        }

        val h = blockchainConfiguration.decodeBlockHeader(header)
        val peerBestHeight = getHeight(h)

        if (peerBestHeight != j.height) {
            headerTrace("Peer for $j drained", peerBestHeight)
            // The peer didn't have the block we wanted
            // Remember its height and try another peer
            peerStatuses.drained(peerId, peerBestHeight)
            restartJob(j)
            return false
        }

        val w = blockchainConfiguration.decodeWitness(witness)
        if ((blockchainConfiguration as BaseBlockchainConfiguration).verifyBlockHeader(h, w)) {
            j.header = h
            j.witness = w
            headerTrace("Header for $j received")
            peerStatuses.headerReceived(peerId, peerBestHeight)
            return true
        } else {
            // There may be two reasons for verification failures.
            // 1. The peer is a scumbag, sending us invalid headers
            // 2. The header is from a configuration that we haven't activated yet.
            // In both cases we can blacklist the peer:
            //
            // 1. blacklisting scumbags is good
            // 2. The blockchain will restart before requestedHeight is added, so the
            // sync process will restart fresh with new configuration. Worst case is if
            // we download parallelism blocks before restarting.
            val dbg = debugJobString(j, requestedHeight, peerId)
            peerStatuses.maybeBlacklist(peerId, "Synch: Invalid header received. $dbg")
            return false
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
            throw BadDataMistake(BadDataType.BAD_MESSAGE, "Expected BaseBlockHeader")
        }
        val height = getHeight(h)
        val j = jobs[height]
        if (j == null) {
            peerStatuses.maybeBlacklist(peerId, "Synch: Why did we get an unfinished block of height: $height from peer: $peerId ? We didn't ask for it")
            return
        }
        unfinishedTrace("Received for $j")
        var bTrace: BlockTrace? = null
        if (logger.isDebugEnabled) {
            bTrace = BlockTrace.build(null, h.blockRID, height)
        }
        val expectedHeader = j.header

        // Validate everything!
        if (j.block != null) {
            peerStatuses.maybeBlacklist(peerId, "Synch: We got this block height = $height already, why send it again?. $j")
            return
        }

        if (peerId != j.peerId) {
            peerStatuses.maybeBlacklist(peerId, "Synch: We didn't expect $peerId to send us an unfinished block (height = $height). We wanted ${j.peerId} to do it. $j")
            return
        }

        if (expectedHeader == null) {
            peerStatuses.maybeBlacklist(peerId, "Synch: We don't have a header yet, why does $peerId send us an unfinished block (height = $height )? $j")
            return
        }

        if (!(expectedHeader.rawData contentEquals header)) {
            peerStatuses.maybeBlacklist(peerId, "Synch: Peer: ${j.peerId} is sending us an unfinished block (height = $height) with a header that doesn't match the header we expected. $j")
            return
        }

        // The witness has already been verified in handleBlockHeader().
        j.block = BlockDataWithWitness(h, txs, j.witness!!)

        commitJobsAsNecessary(bTrace)
    }

    private fun commitJobsAsNecessary(bTrace: BlockTrace?) {
        // We have to make sure blocks are committed in the correct order. If we are missing a block we have to wait for it.
        for (job in jobs.values) {
            // The values are iterated in key-ascending order (see TreeMap)
            if (job.block == null) {
                // The next block to be committed hasn't arrived yet
                unfinishedTrace("Done. Next job, $job, to commit hasn't arrived yet.")
                return
            }
            if (!job.blockCommitting) {
                unfinishedTrace("Committing block for $job")
                job.blockCommitting = true
                commitBlock(job, bTrace)
            }
        }
    }

    /**
     * This is used for syncing from old nodes that doesn't have this new FastSynchronizer algorithm
     */
    private fun handleCompleteBlock(peerId: XPeerID, blockData: MessageBlockData, height: Long, witness: ByteArray) {
        // We expect height to be the requested height. If the peer didn't have the block we wouldn't
        // get any block at all.
        if (!peerStatuses.isMaybeLegacy(peerId)) {
            // We only expect CompleteBlock from legacy nodes.
            return
        }

        val saveBlock = handleBlockHeader(peerId, blockData.header, witness, height)
        if (!saveBlock) {
            return
        }
        handleUnfinishedBlock(peerId, blockData.header, blockData.transactions)
    }

    /**
     * Requirements:
     * 1. Must commit in order of height.
     * 2. If one block fails to commit, we should not commit of blocks coming after it.
     *
     * Therefore:
     * we cannot start all commits in parallel, since that might cause us to commit a block even though
     * previous block failed to commit. Instead we start next commit only after the previous commit was
     * successfully finished.
     */
    private fun commitBlock(job: Job, bTrace: BlockTrace?) {
        if (shutdown.get()) return

        if (addBlockCompletionPromise?.isDone() == true) {
            addBlockCompletionPromise = null
        }

        // We are free to commit this Job, go on and add it to DB
        // (this is usually slow and is therefore handled via a promise).
        val p = blockDatabase.addBlock(job.block!!, addBlockCompletionPromise, bTrace)
        addBlockCompletionPromise = p
        p.success { _ ->
            finishedJobs.add(job)
        }
        p.fail {
            // We got an invalid block from peer. Let's blacklist this
            // peer and try another peer
            if (it is PmEngineIsAlreadyClosed || it is BDBAbortException) {
                warn("Exception committing block $job: ${it.message}")
            } else {
                warn("Exception committing block $job", it)
            }
            job.addBlockException = it
            finishedJobs.add(job)
        }
    }

    private fun processMessages() {
        for (packet in communicationManager.getPackets()) {
            val peerId = packet.first
            if (peerStatuses.isBlacklisted(peerId)) {
                continue
            }
            val message = packet.second
            if (message is GetBlockHeaderAndBlock || message is BlockHeaderMessage) {
                peerStatuses.confirmModern(peerId)
            }
            try {
                when (message) {
                    is GetBlockAtHeight -> sendBlockAtHeight(peerId, message.height)
                    is GetBlockHeaderAndBlock -> sendBlockHeaderAndBlock(peerId, message.height, blockHeight)
                    is BlockHeaderMessage -> handleBlockHeader(peerId, message.header, message.witness, message.requestedHeight)
                    is UnfinishedBlock -> handleUnfinishedBlock(peerId, message.header, message.transactions)
                    is CompleteBlock -> handleCompleteBlock(peerId, message.data, message.height, message.witness)
                    is Status -> peerStatuses.statusReceived(peerId, message.height - 1)
                    else -> trace("Unhandled type ${message} from peer $peerId")
                }
            } catch (e: Exception) {
                logger.info("Couldn't handle message $message from peer $peerId. Ignoring and continuing", e)
            }
        }
    }

    // -------------
    // Only logging below
    // -------------

    fun trace(message: String, e: Exception? = null) {
        if (logger.isTraceEnabled) {
            logger.trace("${workerContext.processName}: $message", e)
        }
    }

    fun error(message: String, e: Exception? = null) {
        logger.error("${workerContext.processName}: $message", e)
    }

    fun warn(message: String, e: Exception? = null) {
        logger.warn("${workerContext.processName}: $message", e)
    }

    // syncUntil()
    private fun syncDebug(message: String, e: Exception? = null) {
        if (logger.isDebugEnabled) {
            logger.debug("${workerContext.processName}: syncUntil() -- $message", e)
        }
    }

    private fun syncDebug(message: String, height: Long, e: Exception? = null) {
        if (logger.isDebugEnabled) {
            logger.debug("${workerContext.processName}: syncUntil() -- $message, at height: $height", e)
        }
    }

    // processDoneJob()
    private fun doneTrace(message: String, e: Exception? = null) {
        if (logger.isTraceEnabled) {
            logger.trace("${workerContext.processName}: processDoneJob() --- $message", e)
        }
    }

    private fun doneDebug(message: String, e: Exception? = null) {
        if (logger.isDebugEnabled) {
            logger.debug("${workerContext.processName}: processDoneJob() -- $message", e)
        }
    }

    private fun doneInfo(message: String, e: Exception? = null) {
        logger.info("${workerContext.processName}: processDoneJob() - $message", e)
    }

    private fun doneError(message: String, e: Exception? = null) {
        logger.error("${workerContext.processName}: processDoneJob() $message", e)
    }

    // startJob()
    private fun startJobTrace(message: String, e: Exception? = null) {
        if (logger.isTraceEnabled) {
            logger.trace("${workerContext.processName}: startJob() -- $message", e)
        }
    }

    // addJob()
    private fun addTrace(message: String, e: Exception? = null) {
        if (logger.isTraceEnabled) {
            logger.trace("${workerContext.processName}: addJob() -- $message", e)
        }
    }

    // handleUnfinishedBlock()
    private fun unfinishedTrace(message: String, e: Exception? = null) {
        if (logger.isTraceEnabled) {
            logger.trace("${workerContext.processName}: handleUnfinishedBlock() -- $message", e)
        }
    }

    private fun headerTrace(message: String, e: Exception? = null) {
        if (logger.isTraceEnabled) {
            logger.trace("${workerContext.processName}: handleBlockHeader() -- $message", e)
        }
    }

    private fun headerTrace(message: String, height: Long, e: Exception? = null) {
        if (logger.isTraceEnabled) {
            logger.trace("${workerContext.processName}: handleBlockHeader() -- $message, at height: $height", e)
        }
    }

}

