package net.postchain.base

import mu.KLogging
import net.postchain.StorageBuilder
import net.postchain.base.data.DatabaseAccess
import net.postchain.config.blockchain.BlockchainConfigurationProvider
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.core.*
import net.postchain.devtools.PeerNameHelper.peerName
import net.postchain.ebft.EBFTSynchronizationInfrastructure
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer

class BaseBlockchainProcessManager(
        private val blockchainInfrastructure: BlockchainInfrastructure,
        private val nodeConfigProvider: NodeConfigurationProvider,
        private val blockchainConfigProvider: BlockchainConfigurationProvider
) : BlockchainProcessManager {

    val nodeConfig = nodeConfigProvider.getConfiguration()
    val storage = StorageBuilder.buildStorage(nodeConfig, NODE_ID_TODO)
    private val blockchainProcesses = mutableMapOf<Long, BlockchainProcess>()
    // FYI: [et]: For integration testing. Will be removed or refactored later
    private val blockchainProcessesLoggers = mutableMapOf<Long, Timer>()
    private val executor = Executors.newSingleThreadExecutor()

    companion object : KLogging()

    override fun startBlockchain(chainId: Long) {
        stopBlockchain(chainId)

        logger.info("[${nodeName()}]: Starting of Blockchain: chainId:$chainId")

        withReadConnection(storage, chainId) { eContext ->
            val configuration = blockchainConfigProvider.getConfiguration(chainId)
            if (configuration != null) {
                val blockchainRID = DatabaseAccess.of(eContext).getBlockchainRID(eContext)!! // TODO: [et]: Fix Kotlin NPE
                val context = BaseBlockchainContext(blockchainRID, NODE_ID_AUTO, chainId, null)

                val blockchainConfig = blockchainInfrastructure.makeBlockchainConfiguration(configuration, context)
                logger.debug { "[${nodeName()}]: BlockchainConfiguration has been created: chainId:$chainId" }

                val engine = blockchainInfrastructure.makeBlockchainEngine(blockchainConfig)
                logger.debug { "[${nodeName()}]: BlockchainEngine has been created: chainId:$chainId" }

                blockchainProcesses[chainId] = blockchainInfrastructure.makeBlockchainProcess(nodeName(), engine) {
                    executor.execute {
                        startBlockchain(chainId)
                    }
                }
                logger.debug { "[${nodeName()}]: BlockchainProcess has been launched: chainId:$chainId" }

                blockchainProcessesLoggers[chainId] = timer(
                        period = 3000,
                        action = { logPeerTopology(chainId) }
                )
                logger.info("[${nodeName()}]: Blockchain has been started: chainId:$chainId")

            } else {
                logger.error("[${nodeName()}]: Can't start Blockchain chainId:$chainId due to configuration is absent")
            }

            Unit
        }
    }

    override fun retrieveBlockchain(chainId: Long): BlockchainProcess? {
        return blockchainProcesses[chainId]
    }

    override fun stopBlockchain(chainId: Long) {
        blockchainProcesses.remove(chainId)?.also {
            logger.info("[${nodeName()}]: Stopping of Blockchain: chainId:$chainId")
            it.shutdown()
        }

        blockchainProcessesLoggers.remove(chainId)?.also {
            it.cancel()
            it.purge()
        }
    }

    override fun shutdown() {
        executor.shutdownNow()
        executor.awaitTermination(1000, TimeUnit.MILLISECONDS)
        blockchainProcesses.forEach { _, process -> process.shutdown() }
        blockchainProcesses.clear()
        blockchainProcessesLoggers.forEach { _, t ->
            t.cancel()
            t.purge()
        }
        storage.close()
        blockchainInfrastructure.shutdown()
    }

    private fun nodeName(): String {
        return peerName(nodeConfig.pubKey)
    }

    // FYI: [et]: For integration testing. Will be removed or refactored later
    private fun logPeerTopology(chainId: Long) {
        // TODO: [et]: Fix links to EBFT entities
        val topology = ((blockchainInfrastructure as BaseBlockchainInfrastructure)
                .synchronizationInfrastructure as EBFTSynchronizationInfrastructure)
                .connectionManager.getPeersTopology(chainId)
                .mapKeys {
                    peerName(it.key)
                }

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