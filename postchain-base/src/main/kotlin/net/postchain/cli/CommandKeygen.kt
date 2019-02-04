package net.postchain.cli

import com.beust.jcommander.Parameters
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.secp256k1_derivePubKey
import net.postchain.common.toHex

@Parameters(commandDescription = "Generates public/private key pair")
class CommandKeygen : Command {

    override fun key(): String = "keygen"

    override fun execute(): CliResult {
        val keys = keygen()
        return Ok(keys)
    }

    /**
     * Cryptographic key generator. Will generate a pair of public and private keys and print to stdout.
     */
    private fun keygen(): String {
        val cs = SECP256K1CryptoSystem()
        // check that privkey is between 1 - 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364140 to be valid?
        val privKey = cs.getRandomBytes(32)
        val pubKey = secp256k1_derivePubKey(privKey)
        return """
            |privkey:   ${privKey.toHex()}
            |pubkey:    ${pubKey.toHex()}
        """.trimMargin()
    }
}
