// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.base.BasePeerCommConfiguration
import net.postchain.base.DynamicPortPeerInfo
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.hexStringToByteArray
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.ebft.BlockchainInstanceModel
import net.postchain.ebft.EBFTBlockchainInstanceWorker
import net.postchain.ebft.makeConnManager
import net.postchain.ebft.message.EbftMessage
import net.postchain.network.PeerConnectionManager
import org.apache.commons.configuration2.Configuration

/**
 * Legacy node using old configuration, used only for testing
 *
 * @property connManager instance of [PeerConnectionManager]
 * @property blockchainInstance instance of [EBFTBlockchainInstance]
 */
class LegacyTestNode {

    lateinit var connManager: PeerConnectionManager<EbftMessage>
    lateinit var blockchainInstance: EBFTBlockchainInstanceWorker

    fun stop() {
        connManager.stop()
        blockchainInstance.shutdown()
    }

    fun getModel(): BlockchainInstanceModel {
        return blockchainInstance
    }

    /**
     * Start the postchain node by setting up everything and finally starting the updateLoop thread
     *
     * @param config configuration settings for the node
     * @param nodeIndex the index of the node
     */
    fun start(config: Configuration, nodeIndex: Int) {
        // This will eventually become a list of chain ids.
        // But for now it's just a single integer.
        val chainId = config.getLong("activechainids")
        val peerInfos = createPeerInfos(config)
        val privKey = config.getString("messaging.privkey").hexStringToByteArray()
        val blockchainRID = config.getString("blockchain.${chainId}.blockchainrid").hexStringToByteArray() // TODO
        val commConfiguration = BasePeerCommConfiguration(peerInfos, blockchainRID, nodeIndex, SECP256K1CryptoSystem(), privKey)

        connManager = makeConnManager(commConfiguration)
        val testNodeEngine = createDataLayer(config, chainId, nodeIndex)
        blockchainInstance = EBFTBlockchainInstanceWorker(
                testNodeEngine.engine,
                config,
                nodeIndex,
                commConfiguration,
                connManager,
                {}
        )
    }

    /**
     * Retrieve peer information from config, including networking info and public keys
     *
     * @param config configuration
     * @return peer information
     */
    fun createPeerInfos(config: Configuration): Array<PeerInfo> {
        // this is for testing only. We can prepare the configuration with a
        // special Array<PeerInfo> for dynamic ports
        val peerInfos = config.getProperty("testpeerinfos")
        if (peerInfos != null) {
            return if (peerInfos is PeerInfo) {
                arrayOf(peerInfos)
            } else {
                (peerInfos as List<PeerInfo>).toTypedArray()
            }
        }

        var peerCount = 0
        config.getKeys("node").forEach { peerCount++ }
        peerCount /= 4

        return Array(peerCount) {
            val port = config.getInt("node.$it.port")
            val host = config.getString("node.$it.host")
            val pubKey = config.getString("node.$it.pubkey").hexStringToByteArray()
            if (port == 0) {
                DynamicPortPeerInfo(host, pubKey)
            } else {
                PeerInfo(host, port, pubKey)
            }
        }
    }

    /**
     * Pre-start function used to process the configuration file before calling the final [start] function
     *
     * @param configFile configuration file to parse
     * @param nodeIndex index of the local node
     */
    fun start(configFile: String, nodeIndex: Int) {
        start(CommonsConfigurationFactory.readFromFile(configFile), nodeIndex)
    }

}