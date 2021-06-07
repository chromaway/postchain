// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools

import net.postchain.base.secp256k1_derivePubKey
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex

/**
 * A cache that maps an "index" to pairs of pub and private keys (The "index" is a "node index" in the context
 * of Postchain, but could in theory be anything.)
 * The smart thing about this cache is that if the pub/priv keys are not known they are calculated from the index and
 * then put into the chache.
 *
 * Note: Clearly this class should only be used for tests. In real code the keys should not be calculated from an index.
 */
object KeyPairHelper {

    private val privKeys = mutableMapOf<Int, Pair<ByteArray, String>>()
    private val pubKeys = mutableMapOf<Int, Pair<ByteArray, String>>()
    private val pubKeyHexToIndex = mutableMapOf<String, Int>()

    // TODO Olle POS-114 Note A bit sad that I had to do this, but it's the usage of [pubKeyFromByteArray()] from BlockchainSetupFactory that breaks
    init {
        for(i in 0..10) {
            pubKey(i)
        }
    }

    fun privKey(pubKey: ByteArray): ByteArray {
        return privKeys[pubKeyHexToIndex[pubKey.toHex()]]!!.first
    }

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

    // TODO: [olle] Is there any way to do the same smart calculation? No fun if we return "null" here
    fun pubKeyFromByteArray(pubKeyHex: String): Int? {
        return pubKeyHexToIndex[pubKeyHex]
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
        val foundPubKey = pubKeys[index]
        if (foundPubKey != null)  {
            return foundPubKey
        } else {
            val calculatedPair = secp256k1_derivePubKey(privKey(index)).let { it to it.toHex() }
            updatePubKeyMaps(index, calculatedPair)
            return calculatedPair
        }
    }

    private fun updatePubKeyMaps(index: Int, pair: Pair<ByteArray, String>) {
        pubKeys[index] = pair
        pubKeyHexToIndex[pair.second] = index
    }

}