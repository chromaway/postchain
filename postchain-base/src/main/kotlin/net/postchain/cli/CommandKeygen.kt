package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.secp256k1_derivePubKey
import net.postchain.common.toHex
import org.bitcoinj.crypto.MnemonicCode

@Parameters(commandDescription = "Generates public/private key pair")
class CommandKeygen : Command {


    @Parameter(
            names = ["-m", "--mnemonic"],
            description = "Mnemonic word list, words separated by space, e.g: \"lift employ roast rotate liar holiday sun fever output magnet...\"")
    private var wordList = ""

    override fun key(): String = "keygen"

    override fun execute(): CliResult {
        val keys = keygen()
        return Ok(keys)
    }

    /**
     * Cryptographic key generator. Will generate a pair of public and private keys and print to stdout.
     */
    private fun keygen(): String {
        var privKey = ByteArray(0)
        var mnemonic : String = ""
        val mnemonicInstance = MnemonicCode.INSTANCE
        if (wordList.isEmpty()) {
            val cs = SECP256K1CryptoSystem()
            privKey = cs.getRandomBytes(32)
            mnemonic = mnemonicInstance.toMnemonic(privKey).joinToString(" ")
        } else {
            val words = wordList.split(" ")
            mnemonicInstance.check(words)
            mnemonic = wordList
            privKey = mnemonicInstance.toEntropy(words)
        }

        val pubKey = secp256k1_derivePubKey(privKey)
        return """
            |privkey:   ${privKey.toHex()}
            |pubkey:    ${pubKey.toHex()}
            |mnemonic:  ${mnemonic} 
        """.trimMargin()
    }
}
