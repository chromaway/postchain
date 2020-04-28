package net.postchain.e2e.tools

import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.client.core.ConfirmationLevel
import net.postchain.client.core.DefaultSigner
import net.postchain.client.core.GTXTransactionBuilder
import net.postchain.client.core.PostchainClientFactory
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvInteger
import net.postchain.gtv.GtvString

abstract class TxSender(
        val apiUrl: String,
        val blockchainRid: String,
        val privKey: String,
        val pubKey: String
) {

    private val cryptoSystem = SECP256K1CryptoSystem()

    fun postAddPeerAsReplicaTx(peerPubkey: String, host: String, port: Int, peerReplicaPubkey: String) {
        postTx { txBuilder ->
            txBuilder.addOperation(
                    "add_peer_replica",
                    arrayOf(
                            GtvByteArray(peerPubkey.hexStringToByteArray()),
                            GtvString(host),
                            GtvInteger(port.toLong()),
                            GtvByteArray(peerReplicaPubkey.hexStringToByteArray())
                    )
            )
        }
    }

    fun postAddBlockchainConfigurationTx(blockchainConfigData: ByteArray, height: Int) {
        postAddBlockchainConfigurationTx(blockchainRid, blockchainConfigData, height)
    }

    fun postAddBlockchainConfigurationTx(blockchainRid: String, blockchainConfigData: ByteArray, height: Int) {
        postTx { txBuilder ->
            txBuilder.addOperation(
                    "add_blockchain_configuration",
                    arrayOf(
                            GtvByteArray(blockchainRid.hexStringToByteArray()),
                            GtvInteger(height.toLong()),
                            GtvByteArray(blockchainConfigData)
                    )
            )
        }
    }


    protected fun postTx(addOperations: (GTXTransactionBuilder) -> Unit) {
        val nodeResolver = PostchainClientFactory.makeSimpleNodeResolver(apiUrl)
        val sigMaker = cryptoSystem.buildSigMaker(pubKey.hexStringToByteArray(), privKey.hexStringToByteArray())
        val signer = DefaultSigner(sigMaker, pubKey.hexStringToByteArray())

        val txBuilder = PostchainClientFactory.getClient(
                nodeResolver, BlockchainRid.buildFromHex(blockchainRid), signer
        ).makeTransaction()

        addOperations(txBuilder)

        txBuilder.sign(sigMaker)
        txBuilder.postSync(ConfirmationLevel.NO_WAIT)
    }
}