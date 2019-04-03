package net.postchain.ebft

import net.postchain.base.PeerCommConfiguration
import net.postchain.common.toHex
import net.postchain.core.UserMistake
import net.postchain.ebft.message.Identification
import net.postchain.ebft.message.Message
import net.postchain.gtv.*
import net.postchain.network.IdentPacketInfo
import net.postchain.network.PacketConverter
import net.postchain.network.XPacketDecoder
import net.postchain.network.XPacketEncoder

// TODO: [et]: Redesign ident stage
@Deprecated("TODO: [et]: Remove it. Was replaced by pair Encoder/Decoder")
class EbftPacketConverter(val config: PeerCommConfiguration) : PacketConverter<Message> {
    override fun makeIdentPacket(forPeer: ByteArray): ByteArray {
        val bytes = GtvEncoder.encodeGtv(Identification(forPeer, byteArrayOf(),
                System.currentTimeMillis()).toGtv())
        val sigMaker = config.sigMaker()
        val signature = sigMaker.signMessage(bytes) // TODO POS-04_sig I THINK this is one of the cases where we actually sign the data
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
        return encodeAndSign(packet, config.sigMaker())
    }

    // TODO: [et]: Improve the design
    override fun isIdentPacket(bytes: ByteArray): Boolean {
        return decodePacket(bytes) is Identification
    }
}
