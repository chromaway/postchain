package net.postchain.ebft

import net.postchain.base.PeerCommConfiguration
import net.postchain.common.toHex
import net.postchain.core.UserMistake
import net.postchain.ebft.message.Identification
import net.postchain.ebft.message.Message
import net.postchain.gtv.*
import net.postchain.network.IdentPacketInfo
import net.postchain.network.PacketConverter

// TODO: [et]: Redesign ident stage
class EbftPacketConverter(val config: PeerCommConfiguration) : PacketConverter<Message> {
    override fun makeIdentPacket(forPeer: ByteArray): ByteArray {
        val bytes = GtvEncoder.encodeGtv(Identification(forPeer, config.blockchainRID, System.currentTimeMillis()).toGtv())
        val signature = config.signer()(bytes)
        return GtvEncoder.encodeGtv(GtvArray(arrayOf(GtvByteArray(bytes),
                GtvByteArray(config.peerInfo[config.myIndex].pubKey), GtvByteArray(signature.data))))
    }

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

    override fun encodePacket(packet: Message): ByteArray {
        return encodeAndSign(packet, config.signer())
    }

    // TODO: [et]: Improve the design
    override fun isIdentPacket(bytes: ByteArray): Boolean {
        return decodePacket(bytes) is Identification
    }
}