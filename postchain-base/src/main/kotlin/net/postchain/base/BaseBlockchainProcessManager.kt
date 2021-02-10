// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.debug.BlockchainProcessName
import net.postchain.debug.NodeDiagnosticContext
import net.postchain.devtools.PeerNameHelper.peerName
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Will run many chains as [BlockchainProcess]:es and keep them in a map.
 */
open class BaseBlockchainProcessManager(
        protected val blockchainInfrastructure: BlockchainInfrastructure,
        protected val nodeConfigProvider: NodeConfigurationProvider,
        protected val blockchainConfigProvider: BlockchainConfigurationProvider,
        protected val nodeDiagnosticContext: NodeDiagnosticContext
) : BlockchainProcessManager {

    override val synchronizer = Any()

    val nodeConfig = nodeConfigProvider.getConfiguration()
    val storage = StorageBuilder.buildStorage(nodeConfig.appConfig, NODE_ID_TODO)
    protected val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()

    // FYI: [et]: For integration testing. Will be removed or refactored later
    private val blockchainProcessesLoggers = mutableMapOf<Long, Timer>() // TODO: [POS-90]: ?
    protected val executor: ExecutorService = Executors.newSingleThreadScheduledExecutor()

    companion object : KLogging()

    /**
     * Put the startup operation of chainId in the [executor]'s work queue.
     *
     * @param chainId is the chain to start.
     */
    protected fun startBlockchainAsync(chainId: Long) {
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
        return synchronized(synchronizer) {
            try {
                stopBlockchain(chainId)

                logger.info("[${nodeName()}]: Starting of Blockchain: chainId: $chainId")

                withReadWriteConnection(storage, chainId) { eContext ->
                    val configuration = blockchainConfigProvider.getConfiguration(eContext, chainId)
                    if (configuration != null) {

                        val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(
                                configuration, eContext, NODE_ID_AUTO, chainId)

                        val processName = BlockchainProcessName(
                                nodeConfig.pubKey, blockchainConfig.blockchainRid)

                        logger.debug { "$processName: BlockchainConfiguration has been created: chainId: $chainId" }

                        val engine = blockchainInfrastructure.makeBlockchainEngine(processName, blockchainConfig, restartHandler(chainId))
                        logger.debug { "$processName: BlockchainEngine has been created: chainId: $chainId" }

                        /*
                        Definition: cross-fetching is the process of downloading blocks from another blockchain
                        over the peer-to-peer network. This is used when forking a chain when we don't have
                        the old chain locally and we haven't been able to sync using the new chain rid.

                        Problem: in order to cross-fetch blocks, we'd like to get the old blockchain's
                        configuration (to find nodes to connect to). But that's difficult. We don't always
                        have it, and we might not have the most recent configuration.

                        If we don't have that, we can use the current blockchain's configuration to
                        find nodes to sync from, since at least a quorum of the signers from old chain
                        must also be signers of the new chain.

                        To simplify things, we will always use current blockchain configuration to find
                        nodes to cross-fetch from. We'll also use sync-nodes.
                         */

                        var histConf: HistoricBlockchain? = null
                        if (blockchainConfig.effectiveBlockchainRID != blockchainConfig.blockchainRid) {
                            histConf = HistoricBlockchain(blockchainConfig.effectiveBlockchainRID)
                            val db = DatabaseAccess.of(eContext)
                            val historicChainIid = db.getChainId(eContext, blockchainConfig.effectiveBlockchainRID)
                            if (historicChainIid != null) {
                                val histConfBytes = blockchainConfigProvider.getConfiguration(eContext, historicChainIid)
                                if (histConfBytes != null) {
                                    val historicBlockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(histConfBytes, eContext, NODE_ID_READ_ONLY, historicChainIid)
                                    val historicBaseConfig = historicBlockchainConfig as BaseBlockchainConfiguration
                                    val historicBlockQueries = historicBaseConfig.makeBlockQueries(storage)
                                    histConf.historicBlockQueries = historicBlockQueries
                                }
                            }
                        }

                        blockchainProcesses[chainId] = blockchainInfrastructure.makeBlockchainProcess(processName, engine, histConf)
                        logger.debug { "$processName: BlockchainProcess has been launched: chainId: $chainId" }

                        logger.info("$processName: Blockchain has been started: chainId: $chainId")
                        blockchainConfig.blockchainRid

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
        synchronized(synchronizer) {
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
        logger.debug("[${nodeName()}]: Stopping BlockchainProcessManager")
        executor.shutdownNow()
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS)

        blockchainProcesses.forEach {it.value.shutdown()}
        blockchainProcesses.clear()

        blockchainProcessesLoggers.forEach { (_, t) ->
            t.cancel()
            t.purge()
        }

        storage.close()
        logger.debug("[${nodeName()}]: Stopped BlockchainProcessManager")
    }

    /**
     * Checks for configuration changes, and then does a async reboot of the given chain.
     *
     * @return a newly created [RestartHandler]. This method will be much more complex is
     * the sublcass [net.postchain.managed.ManagedBlockchainProcessManager].
     */
    protected open fun restartHandler(chainId: Long): RestartHandler {
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
    private fun logPeerTopology(chainId: Long) {
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