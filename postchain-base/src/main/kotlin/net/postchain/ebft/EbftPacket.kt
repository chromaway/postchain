package net.postchain.ebft

import net.postchain.base.PeerCommConfiguration
import net.postchain.common.toHex
import net.postchain.core.UserMistake
import net.postchain.ebft.message.Message
import net.postchain.ebft.message.Identification
import net.postchain.ebft.message.SignedMessage
import net.postchain.network.*

class EbftPacketEncoder(val config: PeerCommConfiguration, val blockchainRID: ByteArray) : XPacketEncoder<Message> {

    override fun makeIdentPacket(forPeer: ByteArray): ByteArray {
        val bytes = Identification(forPeer, blockchainRID, System.currentTimeMillis()).encode()
        val sigMaker = config.sigMaker()
        val signature = sigMaker.signMessage(bytes) // TODO POS-04_sig I THINK this is one of the cases where we actually sign the data
        return SignedMessage(bytes, config.peerInfo[config.myIndex].pubKey, signature.data).encode()
    }

    override fun encodePacket(packet: Message): ByteArray {
        return encodeAndSign(packet, config.sigMaker())
    }
}

class EbftPacketEncoderFactory : XPacketEncoderFactory<Message> {

    override fun create(config: PeerCommConfiguration, blockchainRID: ByteArray): XPacketEncoder<Message> {
        return EbftPacketEncoder(config, blockchainRID)
    }
}

class EbftPacketDecoder(val config: PeerCommConfiguration) : XPacketDecoder<Message> {

    override fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo {
        val signedMessage = decodeSignedMessage(bytes)
        val message = decodeAndVerify(bytes, signedMessage.pubKey, config.verifier())

        if (message !is Identification) {
            throw UserMistake("Packet was not an Identification. Got ${message::class}")
        }

        if (!config.peerInfo[config.myIndex].pubKey.contentEquals(message.pubKey)) {
            throw UserMistake("'yourPubKey' ${message.pubKey.toHex()} of Identification is not mine")
        }

        return IdentPacketInfo(signedMessage.pubKey, message.blockchainRID, null)
    }

    override fun decodePacket(pubKey: ByteArray, bytes: ByteArray): Message {
        return decodeAndVerify(bytes, pubKey, config.verifier())
    }

    override fun decodePacket(bytes: ByteArray): Message? {
        return decodeAndVerify(bytes, config.verifier())
    }

    // TODO: [et]: Improve the design
    override fun isIdentPacket(bytes: ByteArray): Boolean {
        return decodePacket(bytes) is Identification
    }
}

class EbftPacketDecoderFactory : XPacketDecoderFactory<Message> {

    override fun create(config: PeerCommConfiguration): XPacketDecoder<Message> {
        return EbftPacketDecoder(config)
    }
}