// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.network.netty2

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import net.postchain.base.BlockchainRid
import net.postchain.base.PeerCommConfiguration
import net.postchain.ebft.EbftPacketDecoder
import net.postchain.ebft.EbftPacketEncoder
import net.postchain.ebft.message.Message
import net.postchain.network.x.XConnectorEvents
import net.postchain.network.x.XPacketHandler

class EbftTestContext(val config: PeerCommConfiguration, val blockchainRid: BlockchainRid) {

    val packets: XPacketHandler = mock()

    val events: XConnectorEvents = mock {
        on { onPeerConnected(any()) } doReturn packets
    }

    val peer = NettyConnector<Message>(events)

    fun init() = peer.init(config.myPeerInfo(), EbftPacketDecoder(config))

    fun buildPacketEncoder(): EbftPacketEncoder = EbftPacketEncoder(config, blockchainRid)

    fun buildPacketDecoder(): EbftPacketDecoder = EbftPacketDecoder(config)

    fun encodePacket(message: Message): ByteArray = buildPacketEncoder().encodePacket(message)

    fun decodePacket(bytes: ByteArray): Message = buildPacketDecoder().decodePacket(bytes)!!

    fun shutdown() = peer.shutdown()
}