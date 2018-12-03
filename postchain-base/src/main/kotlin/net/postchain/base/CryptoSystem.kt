package net.postchain.base

import net.postchain.core.Signature

/**
 * Function that will sign some data and return a signature
 * */
typealias Signer = (ByteArray) -> Signature

/**
 * Function that will return a boolean depending on if the data and
 * signature applied to that data will properly verify
 * */
typealias Verifier = (ByteArray, Signature) -> Boolean

/**
 * Cryptosystem implements necessary cryptographic functionalities
 */
interface CryptoSystem {
    fun digest(bytes: ByteArray): ByteArray
    fun makeSigner(pubKey: ByteArray, privKey: ByteArray): Signer
    fun verifyDigest(ddigest: ByteArray, s: Signature): Boolean
    fun makeVerifier(): Verifier
    fun getRandomBytes(size: Int): ByteArray
}
