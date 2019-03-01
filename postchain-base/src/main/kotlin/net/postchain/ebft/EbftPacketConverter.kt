package net.postchain.ebft

import net.postchain.base.PeerCommConfiguration
import net.postchain.common.toHex
import net.postchain.core.UserMistake
import net.postchain.ebft.message.EbftMessage
import net.postchain.ebft.message.Identification
import net.postchain.ebft.message.SignedMessage
import net.postchain.network.IdentPacketInfo
import net.postchain.network.PacketConverter
import net.postchain.network.XPacketDecoder
import net.postchain.network.XPacketEncoder

// TODO: [et]: Redesign ident stage
@Deprecated("TODO: [et]: Remove it. Was replaced by pair Encoder/Decoder")
class EbftPacketConverter(val config: PeerCommConfiguration) : PacketConverter<EbftMessage> {
    override fun makeIdentPacket(forPeer: ByteArray): ByteArray {
        val bytes = Identification(forPeer, byteArrayOf()/*config.blockchainRID*/, System.currentTimeMillis()).encode()
        val signature = config.signer()(bytes)
        return SignedMessage(bytes, config.peerInfo[config.myIndex].pubKey, signature.data).encode()
    }

    override fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo {
        val signedMessage = decodeSignedMessage(bytes)
        val message = decodeAndVerify(bytes, signedMessage.pubKey, config.verifier())

        if (message !is Identification) {
            throw UserMistake("Packet was not an Identification. Got ${message::class}")
        }

        if (!config.peerInfo[config.myIndex].pubKey.contentEquals(message.yourPubKey)) {
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

    override fun encodePacket(packet: EbftMessage): ByteArray {
        return encodeAndSign(packet, config.signer())
    }

    // TODO: [et]: Improve the design
    override fun isIdentPacket(bytes: ByteArray): Boolean {
        return decodePacket(bytes) is Identification
    }
}
