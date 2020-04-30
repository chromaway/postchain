// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft

import net.postchain.base.SigMaker
import net.postchain.base.Verifier
import net.postchain.common.toHex
import net.postchain.core.Signature
import net.postchain.core.UserMistake
import net.postchain.ebft.message.Message
import net.postchain.ebft.message.SignedMessage
import java.util.*

fun encodeAndSign(message: Message, sigMaker: SigMaker): ByteArray {
    val signingBytes = message.encode()
    val signature = sigMaker.signMessage(signingBytes) // TODO POS-04_sig I THINK this is one of the cases where we actually sign the data

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

fun decodeAndVerify(bytes: ByteArray, pubKey: ByteArray, verify: Verifier): Message {
    return tryDecodeAndVerify(bytes, pubKey, verify)
            ?: throw UserMistake("Verification failed")
}

fun decodeAndVerify(bytes: ByteArray, verify: Verifier): Message? {
    val message = SignedMessage.decode(bytes)
    val verified = verify(message.message, Signature(message.pubKey, message.signature))

    return if (verified) Message.decode(message.message) else null
}

fun tryDecodeAndVerify(bytes: ByteArray, pubKey: ByteArray, verify: Verifier): Message? {
    val message = SignedMessage.decode(bytes)
    val verified = Arrays.equals(message.pubKey, pubKey)
            && verify(message.message, Signature(message.pubKey, message.signature))
    return if (verified) Message.decode(message.message)
    else null
}
