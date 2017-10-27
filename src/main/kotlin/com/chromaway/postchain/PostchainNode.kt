package com.chromaway.postchain

import com.chromaway.postchain.api.rest.PostchainModel
import com.chromaway.postchain.api.rest.RestApi
import com.chromaway.postchain.base.BasePeerCommConfiguration
import com.chromaway.postchain.base.PeerInfo
import com.chromaway.postchain.base.SECP256K1CryptoSystem
import com.chromaway.postchain.base.data.BaseTransactionQueue
import com.chromaway.postchain.base.hexStringToByteArray
import com.chromaway.postchain.ebft.BaseBlockDatabase
import com.chromaway.postchain.ebft.BaseBlockManager
import com.chromaway.postchain.ebft.BaseBlockchainEngine
import com.chromaway.postchain.ebft.BaseStatusManager
import com.chromaway.postchain.ebft.SyncManager
import com.chromaway.postchain.ebft.makeCommManager
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
        stopMe.set(true)
        restApi?.stop()
    }

    fun start(configFile: String, nodeIndex: Int) {
        val params = Parameters();
        val builder = FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration::class.java).
                configure(params.properties().
                        setFileName(configFile).
                        setListDelimiterHandler(DefaultListDelimiterHandler(',')))
        val config = builder.getConfiguration()

        // This will eventually become a list of chain ids.
        // But for now it's just a single integer.
        val chainId = config.getInt("activechainids").toLong()

        val blockchainConfiguration = getBlockchainConfiguration(config.subset("blockchain.$chainId"), chainId)

        val storage = baseStorage(config, nodeIndex, false)

        val txQueue = BaseTransactionQueue()

        val engine = BaseBlockchainEngine(blockchainConfiguration, storage,
                chainId, txQueue)

        engine.initializeDB()

        val blockQueries = blockchainConfiguration.makeBlockQueries(storage)

        val blockStrategy = blockchainConfiguration.getBlockBuildingStrategy(blockQueries, txQueue)

        val peerInfos = createPeerInfos(config)

        val bestHeight = blockQueries.getBestHeight().get()
        val statusManager = BaseStatusManager(peerInfos.size, nodeIndex, bestHeight+1)
        val blockDatabase = BaseBlockDatabase(engine, blockQueries, nodeIndex)
        val blockManager = BaseBlockManager(blockDatabase, statusManager, blockStrategy)

        val privKey = config.getString("messaging.privkey").hexStringToByteArray()

        val commConfiguration = BasePeerCommConfiguration(peerInfos, nodeIndex, SECP256K1CryptoSystem(), privKey)
        val commManager = makeCommManager(commConfiguration)

        val port = config.getInt("api.port", 7740)
        if (port != -1) {
            val model = PostchainModel(txQueue, blockchainConfiguration.getTransactionFactory(), blockQueries)
            val basePath = config.getString("api.basepath", "")
            restApi = RestApi(model, port, basePath)
        }

        val syncManager = SyncManager(statusManager, blockManager, blockDatabase, commManager, blockchainConfiguration)
        statusManager.recomputeStatus()
        startUpdateLoop(syncManager)
    }

    fun createPeerInfos(config: Configuration): Array<PeerInfo> {
        var peerCount = 0;
        config.getKeys("node").forEach { peerCount++ }
        peerCount = peerCount/4
        return Array(peerCount, {
            PeerInfo(
                    config.getString("node.$it.host"),
                    config.getInt("node.$it.port"),
                    config.getString("node.$it.pubkey").hexStringToByteArray()
            )}
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