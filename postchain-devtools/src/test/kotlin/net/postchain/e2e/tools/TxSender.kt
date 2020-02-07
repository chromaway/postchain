package net.postchain.e2e.tools

import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.client.core.ConfirmationLevel
import net.postchain.client.core.DefaultSigner
import net.postchain.client.core.PostchainClientFactory
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvString

class TxSender(
        val apiUrl: String,
        val blockchainRid: String,
        val privKey: String,
        val pubKey: String
) {

    private val cryptoSystem = SECP256K1CryptoSystem()

    fun postTx(opName: String, vararg args: String) {
        val nodeResolver = PostchainClientFactory.makeSimpleNodeResolver(apiUrl)
        val sigMaker = cryptoSystem.buildSigMaker(pubKey.hexStringToByteArray(), privKey.hexStringToByteArray())
        val signer = DefaultSigner(sigMaker, pubKey.hexStringToByteArray())

        val txBuilder = PostchainClientFactory.getClient(
                nodeResolver, BlockchainRid.buildFromHex(blockchainRid), signer
        ).makeTransaction()

        txBuilder.addOperation(opName, args.map(::GtvString).toTypedArray())
        txBuilder.sign(sigMaker)
        txBuilder.postSync(ConfirmationLevel.NO_WAIT)
    }
}