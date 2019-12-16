package net.postchain.base

import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.debug.NodeDiagnosticContext
import net.postchain.devtools.PeerNameHelper.peerName
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.timer
import kotlin.concurrent.withLock

/**
 * Will run many chains as [BlockchainProcess]:es and keep them in a map.
 */
open class BaseBlockchainProcessManager(
        protected val blockchainInfrastructure: BlockchainInfrastructure,
        protected val nodeConfigProvider: NodeConfigurationProvider,
        protected val blockchainConfigProvider: BlockchainConfigurationProvider,
        protected val nodeDiagnosticContext: NodeDiagnosticContext
) : BlockchainProcessManager {

    override var synchronizer: Lock = ReentrantLock()

    val nodeConfig = nodeConfigProvider.getConfiguration()
    val storage = StorageBuilder.buildStorage(nodeConfig.appConfig, NODE_ID_TODO)
    protected val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()
    // FYI: [et]: For integration testing. Will be removed or refactored later
    private val blockchainProcessesLoggers = mutableMapOf<Long, Timer>() // TODO: [POS-90]: ?
    protected val executor: ExecutorService = Executors.newSingleThreadScheduledExecutor()

    companion object : KLogging()

    /**
     * Put the startup operation of chainId in the [Executor]'s work queue.
     *
     * @param chainId is the chain to start.
     */
    override fun startBlockchainAsync(chainId: Long) {
        executor.execute {
            try {
                startBlockchain(chainId)
            } catch (e: Exception) {
                logger.error(e) { e.message }
            }
        }
    }

    /**
     * Will stop the chain and then start it as a [BlockchainProcess].
     *
     * @param chainId is the chain to start
     * @return the Blockchain's RID if successful, else null
     */
    override fun startBlockchain(chainId: Long): BlockchainRid? {
        return synchronizer.withLock {
            try {
                stopBlockchain(chainId)

                logger.info("[${nodeName()}]: Starting of Blockchain: chainId: $chainId")

                withReadConnection(storage, chainId) { eContext ->
                    val configuration = blockchainConfigProvider.getConfiguration(eContext, chainId)
                    if (configuration != null) {

                        val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(
                                configuration,
                                eContext,
                                NODE_ID_AUTO,
                                chainId)

                        logger.debug { "[${nodeName()}]: BlockchainConfiguration has been created: chainId: $chainId" }

                        val engine = blockchainInfrastructure.makeBlockchainEngine(blockchainConfig, restartHandler(chainId))
                        logger.debug { "[${nodeName()}]: BlockchainEngine has been created: chainId: $chainId" }

                        blockchainProcesses[chainId] = blockchainInfrastructure.makeBlockchainProcess(nodeName(), engine)
                        logger.debug { "[${nodeName()}]: BlockchainProcess has been launched: chainId: $chainId" }

                        blockchainProcessesLoggers[chainId] = timer(
                                period = 3000,
                                action = { logPeerTopology(chainId) }
                        )
                        logger.info("[${nodeName()}]: Blockchain has been started: chainId: $chainId")
                        blockchainConfig.blockchainRID

                    } else {
                        logger.error("[${nodeName()}]: Can't start Blockchain chainId: $chainId due to configuration is absent")
                        null
                    }

                }

            } catch (e: Exception) {
                logger.error(e) { e.message }
                null
            }
        }
    }

    override fun retrieveBlockchain(chainId: Long): BlockchainProcess? {
        return blockchainProcesses[chainId]
    }

    /**
     * Will call "shutdown()" on the [BlockchainProcess] and remove it from the list.
     *
     * @param chainId is the chain to be stopped.
     */
    override fun stopBlockchain(chainId: Long) {
        synchronizer.withLock {
            logger.info("[${nodeName()}]: Stopping of Blockchain: chainId: $chainId")

            blockchainProcesses.remove(chainId)?.also {
                it.shutdown()
            }

            blockchainProcessesLoggers.remove(chainId)?.also {
                it.cancel()
                it.purge()
            }

            logger.info("[${nodeName()}]: Blockchain has been stopped: chainId: $chainId")
        }
    }

    override fun shutdown() {
        executor.shutdownNow()
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS)

        blockchainProcesses.forEach { (_, process) -> process.shutdown() }
        blockchainProcesses.clear()

        blockchainProcessesLoggers.forEach { (_, t) ->
            t.cancel()
            t.purge()
        }

        storage.close()
    }

    /**
     * Checks for configuration changes, and then does a async reboot of the given chain.
     *
     * @return a newly created [RestartHandler]. This method will be much more complex is
     * the sublcass [ManagedBlockchainProcessManager].
     */
    override fun restartHandler(chainId: Long): RestartHandler {
        return {
            val doRestart = withReadConnection(storage, chainId) { eContext ->
                blockchainConfigProvider.needsConfigurationChange(eContext, chainId)
            }

            if (doRestart) {
                startBlockchainAsync(chainId)
            }

            doRestart
        }
    }

    private fun nodeName(): String {
        return peerName(nodeConfig.pubKey)
    }

    // FYI: [et]: For integration testing. Will be removed or refactored later
    fun logPeerTopology(chainId: Long) {
        // TODO: [et]: Fix links to EBFT entities
        val topology = ((blockchainInfrastructure as BaseBlockchainInfrastructure)
                .synchronizationInfrastructure as? EBFTSynchronizationInfrastructure)
                ?.connectionManager?.getPeersTopology(chainId)
                ?.mapKeys {
                    peerName(it.key)
                }
                ?: emptyMap()

        val prettyTopology = topology.mapValues {
            it.value
                    .replace("c>", "${nodeName()}>")
                    .replace("s<", "${nodeName()}<")
                    .replace("<c", "<${peerName(it.key)}")
                    .replace(">s", ">${peerName(it.key)}")
        }

        logger.trace {
            "[${nodeName()}]: Topology: ${prettyTopology.values}"
        }
    }
}