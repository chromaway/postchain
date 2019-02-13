// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.ebft

import net.postchain.base.Signer
import net.postchain.base.Verifier
import net.postchain.common.toHex
import net.postchain.core.Signature
import net.postchain.core.UserMistake
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.message.SignedMessage
import java.util.*

fun encodeAndSign(message: EbftMessage, sign: Signer): ByteArray {
    val signingBytes = message.encode()
    val signature = sign(signingBytes)
    return SignedMessage(signingBytes, signature.subjectID, signature.data).encode()
}

fun decodeSignedMessage(bytes: ByteArray): SignedMessage {
    try {
        return SignedMessage.decode(bytes)
    } catch (e: Exception) {
        throw UserMistake("bytes ${bytes.toHex()} cannot be decoded", e)
    }
}

fun decodeWithoutVerification(bytes: ByteArray): SignedMessage {
    try {
        return SignedMessage.decode(bytes)
    } catch (e: Exception) {
        throw UserMistake("bytes cannot be decoded", e)
    }
}

fun decodeAndVerify(bytes: ByteArray, pubKey: ByteArray, verify: Verifier): EbftMessage {
    return tryDecodeAndVerify(bytes, pubKey, verify)
            ?: throw UserMistake("Verification failed")
}

fun decodeAndVerify(bytes: ByteArray, verify: Verifier): EbftMessage? {
    val message = SignedMessage.decode(bytes)
    val verified = verify(message.message, Signature(message.pubKey, message.signature))
    return if (verified) EbftMessage.decode(message.message) else null
}

fun tryDecodeAndVerify(bytes: ByteArray, pubKey: ByteArray, verify: Verifier): EbftMessage? {
    val message = SignedMessage.decode(bytes)
    val verified = Arrays.equals(message.pubKey, pubKey)
            && verify(message.message, Signature(message.pubKey, message.signature))
    return if (verified) EbftMessage.decode(message.message) else null
}
