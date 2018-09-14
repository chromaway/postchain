package net.postchain.base

import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.common.hexStringToByteArray
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.NetworkInfrastructure
import net.postchain.ebft.CommManager
import net.postchain.ebft.createPeerInfos
import net.postchain.ebft.makeCommManager
import net.postchain.ebft.makeConnManager
import net.postchain.ebft.message.EbftMessage
import org.apache.commons.configuration2.Configuration

class BaseNetworkInfrastructure(val config: Configuration) : NetworkInfrastructure {

    override val peers = createPeerInfos(config)

    override fun buildCommunicationManager(configuration: BlockchainConfiguration): CommManager<EbftMessage> {
        val blockchainConfig = (configuration as BaseBlockchainConfiguration) // TODO: [et]: Resolve type cast

        val communicationConfig = BasePeerCommConfiguration(
                peers,
                blockchainConfig.blockchainRID,
                blockchainConfig.configData.context.nodeID,
                SECP256K1CryptoSystem(),
                privKey())

        val connectionManager = makeConnManager(communicationConfig)
        return makeCommManager(communicationConfig, connectionManager)
    }

    private fun privKey(): ByteArray =
            config.getString("messaging.privkey").hexStringToByteArray()
}