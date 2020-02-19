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
import kotlin.random.Random

class TxSender(
        val apiUrl: String,
        val blockchainRid: String,
        val privKey: String,
        val pubKey: String
) {

    private val cryptoSystem = SECP256K1CryptoSystem()

    fun postNopTx() {
        postTx { txBuilder ->
            val nonce = Random.Default.nextInt(1000).toString()
            txBuilder.addOperation("nop", arrayOf(GtvString(nonce)))
        }
    }

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

    private fun postTx(addOperations: (GTXTransactionBuilder) -> Unit) {
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