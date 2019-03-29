package net.postchain.config.node

import net.postchain.base.PeerInfo
import net.postchain.common.hexStringToByteArray
import net.postchain.config.app.AppConfig
import org.apache.commons.configuration2.Configuration

open class NodeConfig(private val appConfig: AppConfig) {

    private val config: Configuration
        get() = appConfig.config

    /**
     * Blockchain configuration provider
     */
    val blockchainConfigProvider: String
        get() = config.getString("configuration.provider.blockchain") // manual | managed

    val infrastructure: String
        get() = config.getString("infrastructure") // base/ebft | base/test


    /**
     * Database
     */
    val databaseDriverclass: String
        get() = config.getString("database.driverclass")

    val databaseUrl: String
        get() = config.getString("database.url")

    val databaseSchema: String
        get() = config.getString("database.schema", "public")

    val databaseUsername: String
        get() = config.getString("database.username")

    val databasePassword: String
        get() = config.getString("database.password")


    /**
     * Pub/Priv keys
     */
    val privKey: String
        get() = config.getString("messaging.privkey")

    val privKeyByteArray: ByteArray
        get() = privKey.hexStringToByteArray()

    val pubKey: String
        get() = config.getString("messaging.pubkey")

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
    open val peerInfos: Array<PeerInfo> = arrayOf()


    /**
     * Active Chains
     */
    val activeChainIds: Array<String>
        get() = config.getStringArray("activechainids")

}