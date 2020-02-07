package net.postchain.client

import net.postchain.base.BlockchainRid
import net.postchain.common.PropertiesFileLoader
import net.postchain.common.hexStringToByteArray
import org.apache.commons.configuration2.Configuration

class AppConfig(private val config: Configuration) {

    companion object {

        fun fromProperties(propertiesFilename: String): AppConfig {
            return AppConfig(PropertiesFileLoader.load(propertiesFilename))
        }
    }

    val apiUrl: String
        get() = System.getenv("POSTCHAIN_CLIENT_API_URL")
                ?: config.getString("api-url", "")

    val blockchainRidStr: String
        get() = System.getenv("POSTCHAIN_CLIENT_BLOCKCHAIN_RID")
                ?: config.getString("blockchain-rid", "")

    val blockchainRid: BlockchainRid
        get() = BlockchainRid.buildFromHex(blockchainRidStr)

    /**
     * Pub/Priv keys
     */
    val privKey: String
        get() = System.getenv("POSTCHAIN_CLIENT_PRIVKEY")
                ?: config.getString("privkey", "")

    val privKeyByteArray: ByteArray
        get() = privKey.hexStringToByteArray()

    val pubKey: String
        get() = System.getenv("POSTCHAIN_CLIENT_PUBKEY")
                ?: config.getString("pubkey", "")

    val pubKeyByteArray: ByteArray
        get() = pubKey.hexStringToByteArray()
}