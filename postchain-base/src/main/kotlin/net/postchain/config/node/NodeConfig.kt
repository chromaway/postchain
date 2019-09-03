package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.common.hexStringToByteArray
import net.postchain.config.app.AppConfig
import net.postchain.network.x.XPeerID
import org.apache.commons.configuration2.Configuration

open class NodeConfig(private val appConfig: AppConfig) {

    private val config: Configuration
        get() = appConfig.config

    /**
     * Blockchain configuration provider
     */
    val blockchainConfigProvider: String
        // manual | managed
        get() = config.getString("configuration.provider.blockchain", "")

    val infrastructure: String
        // base/ebft | base/test
        get() = config.getString("infrastructure", "")


    /**
     * Database
     */
    val databaseDriverclass: String
        get() = appConfig.databaseDriverclass

    val databaseUrl: String
        get() = appConfig.databaseUrl

    val databaseSchema: String
        get() = appConfig.databaseSchema

    val databaseUsername: String
        get() = appConfig.databaseUsername

    val databasePassword: String
        get() = appConfig.databasePassword


    /**
     * Pub/Priv keys
     */
    val privKey: String
        get() = config.getString("messaging.privkey", "")

    val privKeyByteArray: ByteArray
        get() = privKey.hexStringToByteArray()

    val pubKey: String
        get() = config.getString("messaging.pubkey", "")

    val pubKeyByteArray: ByteArray
        get() = pubKey.hexStringToByteArray()


    /**
     * REST API
     */
    val restApiBasePath: String
        get() = config.getString("api.basepath", "")

    val restApiPort: Int
        get() = config.getInt("api.port", 7740)

    val restApiSsl: Boolean
        get() = config.getBoolean("api.enable_ssl", false)

    val restApiSslCertificate: String
        get() = config.getString("api.ssl_certificate", "")

    val restApiSslCertificatePassword: String
        get() = config.getString("api.ssl_certificate.password", "")


    /**
     * Peers
     */
    open val peerInfoMap: Map<XPeerID, PeerInfo> = mapOf()

    /**
     * Active Chains
     */
    val activeChainIds: Array<String>
        get() = config.getStringArray("activechainids")

}