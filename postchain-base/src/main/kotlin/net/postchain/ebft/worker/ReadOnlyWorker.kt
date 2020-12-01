// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.worker

import net.postchain.core.BlockchainEngine
import net.postchain.core.BlockchainProcess
import net.postchain.core.NODE_ID_READ_ONLY
import net.postchain.debug.BlockchainProcessName
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.message.Message
import net.postchain.ebft.syncmanager.common.FastSynchronizer
import net.postchain.network.CommunicationManager
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * A blockchain instance replica worker
 * @property updateLoop the main thread
 */
class ReadOnlyWorker(
        val processName: BlockchainProcessName,
        signers: List<ByteArray>,
        private val blockchainEngine: BlockchainEngine,
        private val communicationManager: CommunicationManager<Message>,
        private val onShutdown: () -> Unit = {}
) : BlockchainProcess {

    override fun getEngine() = blockchainEngine

    private val fastSynchronizer: FastSynchronizer

    private val done = CountDownLatch(1)

    init {
        val blockDatabase = BaseBlockDatabase(
                blockchainEngine, blockchainEngine.getBlockQueries(), NODE_ID_READ_ONLY)

        fastSynchronizer = FastSynchronizer(
                communicationManager,
                blockDatabase,
                blockchainEngine.getConfiguration(),
                blockchainEngine.getBlockQueries())
        thread(name = "replicaSync-$processName") {
            fastSynchronizer.syncUntilShutdown()
            done.countDown()
        }
    }

    override fun shutdown() {
        communicationManager.shutdown()
        fastSynchronizer.shutdown()
        done.await()
        onShutdown()
    }
}