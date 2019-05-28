package net.postchain.ebft

import net.postchain.base.PeerCommConfiguration
import net.postchain.common.toHex
import net.postchain.core.UserMistake
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.message.Identification
import net.postchain.ebft.message.SignedMessage
import net.postchain.network.*

class EbftPacketEncoder(val config: PeerCommConfiguration, val blockchainRID: ByteArray) : XPacketEncoder<EbftMessage> {

    override fun makeIdentPacket(forPeer: ByteArray): ByteArray {
        val bytes = Identification(forPeer, blockchainRID, System.currentTimeMillis()).encode()
        val signature = config.signer()(bytes)
        return SignedMessage(bytes, config.pubKey, signature.data).encode()
    }

    override fun encodePacket(packet: EbftMessage): ByteArray {
        return encodeAndSign(packet, config.signer())
    }
}

class EbftPacketEncoderFactory : XPacketEncoderFactory<EbftMessage> {

    override fun create(config: PeerCommConfiguration, blockchainRID: ByteArray): XPacketEncoder<EbftMessage> {
        return EbftPacketEncoder(config, blockchainRID)
    }
}

class EbftPacketDecoder(val config: PeerCommConfiguration) : XPacketDecoder<EbftMessage> {

    override fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo {
        val signedMessage = decodeSignedMessage(bytes)
        val message = decodeAndVerify(bytes, signedMessage.pubKey, config.verifier())

        if (message !is Identification) {
            throw UserMistake("Packet was not an Identification. Got ${message::class}")
        }

        if (!config.pubKey.contentEquals(message.yourPubKey)) {
            throw UserMistake("'yourPubKey' ${message.yourPubKey.toHex()} of Identification is not mine")
        }

        return IdentPacketInfo(signedMessage.pubKey, message.blockchainRID, null)
    }

    override fun decodePacket(pubKey: ByteArray, bytes: ByteArray): EbftMessage {
        return decodeAndVerify(bytes, pubKey, config.verifier())
    }

    override fun decodePacket(bytes: ByteArray): EbftMessage? {
        return decodeAndVerify(bytes, config.verifier())
    }

    // TODO: [et]: Improve the design
    override fun isIdentPacket(bytes: ByteArray): Boolean {
        return decodePacket(bytes) is Identification
    }
}

class EbftPacketDecoderFactory : XPacketDecoderFactory<EbftMessage> {

    override fun create(config: PeerCommConfiguration): XPacketDecoder<EbftMessage> {
        return EbftPacketDecoder(config)
    }
}