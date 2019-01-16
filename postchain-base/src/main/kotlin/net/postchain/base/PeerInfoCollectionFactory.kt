package net.postchain.base

import net.postchain.common.hexStringToByteArray
import org.apache.commons.configuration2.Configuration

object PeerInfoCollectionFactory {

    /**
     * Retrieve peer information from config, including networking info and public keys
     *
     * @param config configuration
     * @return peer information
     */
    fun createPeerInfoCollection(config: Configuration): Array<PeerInfo> {
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