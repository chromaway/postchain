// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain

import net.postchain.api.rest.Model
import net.postchain.api.rest.PostchainModel
import net.postchain.api.rest.RestApi
import net.postchain.base.*
import net.postchain.base.data.BaseTransactionQueue
import net.postchain.core.BlockBuildingStrategy
import net.postchain.core.BlockQueries
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.Network
import net.postchain.core.TransactionEnqueuer
import net.postchain.ebft.BaseBlockDatabase
import net.postchain.ebft.BaseBlockManager
import net.postchain.ebft.BaseBlockchainEngine
import net.postchain.ebft.BaseStatusManager
import net.postchain.ebft.BlockManager
import net.postchain.ebft.BlockchainEngine
import net.postchain.ebft.CommManager
import net.postchain.ebft.SyncManager
import net.postchain.ebft.makeCommManager
import net.postchain.ebft.message.EbftMessage
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread


class PostchainNode {
    lateinit var updateLoop: Thread
    val stopMe = AtomicBoolean(false)
    var restApi: RestApi? = null
    lateinit var blockchainConfiguration: BlockchainConfiguration
    lateinit var storage: Storage
    lateinit var blockQueries: BlockQueries
    lateinit var peerInfos: Array<PeerInfo>
    lateinit var statusManager: BaseStatusManager
    lateinit var commManager: CommManager<EbftMessage>
    lateinit var network: Network
    lateinit var txQueue: BaseTransactionQueue
    lateinit var txEnqueuer: TransactionEnqueuer
    lateinit var blockStrategy: BlockBuildingStrategy
    lateinit var engine: BlockchainEngine
    lateinit var blockDatabase: BaseBlockDatabase
    lateinit var blockManager: BlockManager
    lateinit var model: Model
    lateinit var syncManager: SyncManager

    protected fun startUpdateLoop(syncManager: SyncManager) {
        updateLoop = thread(name = "updateLoop") {
            while (true) {
                if (stopMe.get()) {
                    break
                }
                syncManager.update()
                Thread.sleep(100)
            }
        }
    }

    fun stop() {
        // Ordering is important.
        // 1. Stop acceptin API calls
        stopMe.set(true)
        restApi?.stop()
        // 2. Close the data sources so that new blocks cant be started
        storage.close()
        // 3. Close the listening port and all TCP connections
        commManager.stop()
        // 4. Stop any in-progress blocks
        blockDatabase.stop()
    }

    fun start(config: Configuration, nodeIndex: Int) {
        // This will eventually become a list of chain ids.
        // But for now it's just a single integer.
        val chainId = config.getInt("activechainids").toLong()

        blockchainConfiguration = getBlockchainConfiguration(config.subset("blockchain.$chainId"), chainId)
        storage = baseStorage(config, nodeIndex)
        blockQueries = blockchainConfiguration.makeBlockQueries(storage)
        peerInfos = createPeerInfos(config)
        txQueue = BaseTransactionQueue()
        engine = BaseBlockchainEngine(blockchainConfiguration, storage,
                chainId, txQueue)

        engine.initializeDB()

        val bestHeight = blockQueries.getBestHeight().get()
        statusManager = BaseStatusManager(peerInfos.size, nodeIndex, bestHeight+1)

        val privKey = config.getString("messaging.privkey").hexStringToByteArray()

        val commConfiguration = BasePeerCommConfiguration(peerInfos, nodeIndex, SECP256K1CryptoSystem(), privKey)
        commManager = makeCommManager(commConfiguration)

        txEnqueuer = NetworkAwareTxEnqueuer(txQueue, commManager, nodeIndex)

        blockStrategy = blockchainConfiguration.getBlockBuildingStrategy(blockQueries, txQueue)

        blockDatabase = BaseBlockDatabase(engine, blockQueries, nodeIndex)
        blockManager = BaseBlockManager(blockDatabase, statusManager, blockStrategy)

        val port = config.getInt("api.port", 7740)
        if (port != -1) {
            model = PostchainModel(txEnqueuer, blockchainConfiguration.getTransactionFactory(),
                    blockQueries as BaseBlockQueries)
            val basePath = config.getString("api.basepath", "")
            restApi = RestApi(model, port, basePath)
        }

        // Give the SyncManager the BaseTransactionQueue and not the network-aware one,
        // because we don't want tx forwarding/broadcasting when received through p2p network
        syncManager = SyncManager(statusManager, blockManager, blockDatabase, commManager, txQueue, blockchainConfiguration)
        statusManager.recomputeStatus()
        startUpdateLoop(syncManager)
    }

    fun start(configFile: String, nodeIndex: Int) {
        val params = Parameters();
        val builder = FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration::class.java).
                configure(params.properties().
                        setFileName(configFile).
                        setListDelimiterHandler(DefaultListDelimiterHandler(',')))
        val config = builder.getConfiguration()
        start(config, nodeIndex)
    }

    fun createPeerInfos(config: Configuration): Array<PeerInfo> {
        // this is for testing only. We can prepare the configuration with a
        // special Array<PeerInfo> for dynamic ports
        val peerInfos = config.getProperty("testpeerinfos")
        if (peerInfos != null) {
            return (peerInfos as List<PeerInfo>).toTypedArray()
        }

        var peerCount = 0;
        config.getKeys("node").forEach { peerCount++ }
        peerCount = peerCount/4
        return Array(peerCount, {
            val port = config.getInt("node.$it.port")
            val host = config.getString("node.$it.host")
            val pubKey = config.getString("node.$it.pubkey").hexStringToByteArray()
            if (port == 0) {
                DynamicPortPeerInfo(host, pubKey)
            } else {
                PeerInfo(host, port, pubKey)
            }
        }

        )
    }
}

/**
 * args: [ { --nodeIndex | -i } <index> ] [ { --config | -c } <configFile> ]
 */
fun main(args: Array<String>) {
    var i = 0
    var nodeIndex = 0;
    var config = ""
    while (i < args.size) {
        when (args[i]) {
            "-i", "--nodeIndex" -> {
                nodeIndex = parseInt(args[++i])!!
            }
            "-c", "--config" -> {
                config = args[++i]
            }
        }
        i++
    }
    if (config == "") {
        config = "config/config.$nodeIndex.properties"
    }
    val node = PostchainNode()
    node.start(config, nodeIndex)
}