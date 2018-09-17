package net.postchain.test

import net.postchain.base.secp256k1_derivePubKey
import net.postchain.common.toHex

class KeyPairHelper {

    companion object {

        fun privKey(index: Int): ByteArray {
            // private key index 0 is all zeroes except byte 16 which is 1
            // private key index 12 is all 12:s except byte 16 which is 1
            // reason for byte16=1 is that private key cannot be all zeroes
            return ByteArray(32) { if (it == 16) 1.toByte() else index.toByte() }
        }

        fun privKeyHex(index: Int): String {
            return privKey(index).toHex()
        }

        fun pubKey(index: Int): ByteArray {
            return secp256k1_derivePubKey(privKey(index))
        }

        fun pubKeyHex(index: Int): String {
            return pubKey(index).toHex()
        }
    }
}