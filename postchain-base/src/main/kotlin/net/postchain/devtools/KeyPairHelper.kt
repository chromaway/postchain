// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools

import net.postchain.base.secp256k1_derivePubKey
import net.postchain.common.toHex

object KeyPairHelper {

    private val privKeys = mutableMapOf<Int, Pair<ByteArray, String>>()
    private val pubKeys = mutableMapOf<Int, Pair<ByteArray, String>>()

    fun privKey(index: Int): ByteArray {
        return getCachedPrivKey(index).first
    }

    fun privKeyHex(index: Int): String {
        return getCachedPrivKey(index).second
    }

    fun pubKey(index: Int): ByteArray {
        return getCachedPubKey(index).first
    }

    fun pubKeyHex(index: Int): String {
        return getCachedPubKey(index).second
    }

    private fun getCachedPrivKey(index: Int): Pair<ByteArray, String> {
        return privKeys.getOrPut(index) {
            // private key index 0 is all zeroes except byte 16 which is 1
            // private key index 12 is all 12:s except byte 16 which is 1
            // reason for byte16=1 is that private key cannot be all zeroes
            ByteArray(32) { if (it == 16) 1.toByte() else index.toByte() }
                    .let { it to it.toHex() }
        }
    }

    private fun getCachedPubKey(index: Int): Pair<ByteArray, String> {
        return pubKeys.getOrPut(index) {
            secp256k1_derivePubKey(privKey(index))
                    .let { it to it.toHex() }
        }
    }
}