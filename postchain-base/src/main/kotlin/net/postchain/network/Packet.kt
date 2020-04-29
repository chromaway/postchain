// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network

import net.postchain.base.BlockchainRid
import net.postchain.base.PeerCommConfiguration
import net.postchain.base.PeerID

interface XPacketEncoder<PacketType> {
    fun makeIdentPacket(forPeer: PeerID): ByteArray
    fun encodePacket(packet: PacketType): ByteArray
}

interface XPacketEncoderFactory<PacketType> {
    fun create(config: PeerCommConfiguration, blockchainRID: BlockchainRid): XPacketEncoder<PacketType>
}

interface XPacketDecoder<PacketType> {
    fun parseIdentPacket(bytes: ByteArray): IdentPacketInfo
    fun decodePacket(pubKey: ByteArray, bytes: ByteArray): PacketType
    fun decodePacket(bytes: ByteArray): PacketType?
    fun isIdentPacket(bytes: ByteArray): Boolean
}

interface XPacketDecoderFactory<PacketType> {
    fun create(config: PeerCommConfiguration): XPacketDecoder<PacketType>
}

data class IdentPacketInfo(val peerID: PeerID,
                           val blockchainRID: BlockchainRid,
                           val sessionKey: ByteArray? = null,
                           val ephemeralPubKey: ByteArray? = null)
