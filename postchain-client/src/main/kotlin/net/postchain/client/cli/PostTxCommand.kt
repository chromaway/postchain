package net.postchain.client.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.client.AppConfig
import net.postchain.client.core.ConfirmationLevel
import net.postchain.client.core.DefaultSigner
import net.postchain.client.core.GTXTransactionBuilder
import net.postchain.client.core.PostchainClientFactory
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvInteger
import net.postchain.gtv.GtvString

/**
 * Cli test command
 */
class PostTxCommand : CliktCommand(name = "post-tx", help = "Posts tx") {

    private val opName by argument()

    private val args by argument().multiple()

    private fun configFile(): String {
        return (context.parent?.command as? Cli)?.configFile ?: ""
    }

    private val cryptoSystem = SECP256K1CryptoSystem()

    override fun run() {
        try {
            val appConfig = AppConfig.fromProperties(configFile())

            postTx(appConfig) {
                it.addOperation(opName, args.map(::encodeArg).toTypedArray())
            }

            println("Tx with the operation has been posted: $opName(${args.joinToString()})")

        } catch (e: Exception) {
            println(e)
        }
    }

    /**
     * Encodes numbers as GtvInteger and strings as GtvString values
     */
    private fun encodeArg(arg: String): Gtv {
        return arg.toLongOrNull()
                ?.let(::GtvInteger)
                ?: GtvString(arg)
    }

    private fun postTx(appConfig: AppConfig, addOperations: (GTXTransactionBuilder) -> Unit) {
        val nodeResolver = PostchainClientFactory.makeSimpleNodeResolver(appConfig.apiUrl)
        val sigMaker = cryptoSystem.buildSigMaker(appConfig.pubKeyByteArray, appConfig.privKeyByteArray)
        val signer = DefaultSigner(sigMaker, appConfig.pubKeyByteArray)
        val client = PostchainClientFactory.getClient(nodeResolver, appConfig.blockchainRid, signer)
        val txBuilder = client.makeTransaction()
        addOperations(txBuilder)
        txBuilder.sign(sigMaker)
        txBuilder.postSync(ConfirmationLevel.NO_WAIT)
    }
}
