package net.postchain.ebft.syncmanager.common

import java.util.HashMap


/**
 * NOTE!!
 * This data will be handled by different threads, so it must be thread safe
 */
class CommitQueue {

    private var queueRunning = false; // "true" if the queue is working on emptying itself.
    private val internalQueue = HashMap<Long, FastSynchronizer.Job>()

    @Synchronized
    fun scheduleForCommit(job: FastSynchronizer.Job) {
        internalQueue[job.height] = job
    }

    @Synchronized
    fun isRunning(): Boolean {
        return queueRunning
    }

    @Synchronized
    fun setRunning(b: Boolean) {
        queueRunning = b
    }

    /**
     * @param syncLoop is "true" if this is the "main thread" kick-starting the process.
     *                 If the queue is already running, we shouldn't start it again.
     * @param nextHeight is the height of the next job we should fetch
     * @return 1) a Job at the given height or 2) nothing if we are not supposed to proceed.
     */
    @Synchronized
    fun isJobReadyForCommit(syncLoop: Boolean, nextHeight: Long): FastSynchronizer.Job? {
        if (syncLoop) {
            if (queueRunning) return null
            queueRunning = true
        }
        val job = internalQueue.remove(nextHeight)
        if (job == null) {
            // We don't have scheduled this job, so queue must be empty
            queueRunning =false
            return null
        }
        return job
    }
}