// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.worker

import mu.KLogging
import net.postchain.core.BlockchainProcess
import net.postchain.core.NODE_ID_READ_ONLY
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.syncmanager.common.FastSyncParameters
import net.postchain.ebft.syncmanager.common.FastSynchronizer
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class ReadOnlyWorker(val workerContext: WorkerContext) : BlockchainProcess {

    companion object : KLogging()

    override fun getEngine() = workerContext.engine

    private val fastSynchronizer: FastSynchronizer

    private val done = CountDownLatch(1)

    private val blockDatabase = BaseBlockDatabase(
            getEngine(), getEngine().getBlockQueries(), NODE_ID_READ_ONLY)

    init {

        val params = FastSyncParameters()
        params.jobTimeout = workerContext.nodeConfig.fastSyncJobTimeout

        fastSynchronizer = FastSynchronizer(workerContext,
                blockDatabase, params)
        thread(name = "replicaSync-${workerContext.processName}") {
            fastSynchronizer.syncUntilShutdown()
            done.countDown()
        }
    }

    fun getHeight(): Long = fastSynchronizer.blockHeight

    override fun shutdown() {
        shutdownDebug("Begin")
        fastSynchronizer.shutdown()
        blockDatabase.stop()
        shutdownDebug("Wait for \"done\"")
        done.await()
        workerContext.shutdown()
        shutdownDebug("End")
    }

    private fun shutdownDebug(str: String) {
        if (logger.isDebugEnabled) {
            logger.debug("${workerContext.processName}: shutdown() - $str.")
        }
    }
}