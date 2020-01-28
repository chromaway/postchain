// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.network.x.XPeerID
import org.apache.commons.configuration2.Configuration

class LegacyNodeConfigurationProvider(private val appConfig: AppConfig) : NodeConfigurationProvider {

    override fun getConfiguration(): NodeConfig {
        return object : NodeConfig(appConfig) {
            override val peerInfoMap = createPeerInfoMap(appConfig.config)
        }
    }

    private fun createPeerInfoMap(config: Configuration): Map<XPeerID, PeerInfo> =
        createPeerInfoCollection(config).map { XPeerID(it.pubKey) to it }.toMap()

    /**
     * Retrieves peer information from config, including networking info and public keys
     */
    private fun createPeerInfoCollection(config: Configuration): Array<PeerInfo> {
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

        // TODO: [et]: Refactor this
        var peerCount = 0
        config.getKeys("node").forEach { peerCount++ }
        peerCount /= 4

        return Array(peerCount) {
            val port = config.getInt("node.$it.port")
            val host = config.getString("node.$it.host")
            val pubKey = config.getString("node.$it.pubkey").hexStringToByteArray()
            PeerInfo(host, port, pubKey)
        }
    }
}