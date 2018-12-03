package net.postchain.network

import net.postchain.base.PeerInfo
import net.postchain.core.byteArrayKeyOf
import org.awaitility.Awaitility.await
import org.easymock.EasyMock.*
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class ConnectionManagerTest {

    private val blockchainRid = byteArrayOf(0x01)

    private lateinit var peerInfo1: PeerInfo
    private lateinit var packetConverter1: PacketConverter<Int>

    private lateinit var peerInfo2: PeerInfo
    private lateinit var packetConverter2: PacketConverter<Int>

    @Before
    fun setUp() {
        // TODO: [et]: Make dynamic ports
        peerInfo1 = PeerInfo("localhost", 3331, byteArrayOf(0x01))
        peerInfo2 = PeerInfo("localhost", 3332, byteArrayOf(0x02))

        packetConverter1 = mock(PacketConverter::class.java) as PacketConverter<Int>
        packetConverter2 = mock(PacketConverter::class.java) as PacketConverter<Int>
    }

    @Test
    fun twoPeers_Connection_And_Sending_Successful() {
        // Given / Peer 1
        expect(packetConverter1.makeIdentPacket(peerInfo2.pubKey))
                .andReturn(byteArrayOf(0x02, 0x02)).times(1)
        expect(packetConverter1.parseIdentPacket(byteArrayOf(0x01, 0x01)))
                .andReturn(IdentPacketInfo(peerInfo1.pubKey, blockchainRid)).times(1)
        expect(packetConverter1.encodePacket(42))
                .andReturn(byteArrayOf(0x02, 0x42, 0x42)).times(2)

        // Given / Peer 2
        expect(packetConverter2.makeIdentPacket(peerInfo1.pubKey))
                .andReturn(byteArrayOf(0x01, 0x01)).times(1)
        expect(packetConverter2.parseIdentPacket(byteArrayOf(0x02, 0x02)))
                .andReturn(IdentPacketInfo(peerInfo2.pubKey, blockchainRid)).times(1)

        // Replay
        replay(packetConverter1)
        replay(packetConverter2)

        // When
        val packets1 = mutableListOf<ByteArray>()
        val connectionManager1 = PeerConnectionManager(peerInfo1, packetConverter1)
        connectionManager1.registerBlockchain(blockchainRid, BlockchainDataReceiver(packets1))

        val packets2 = mutableListOf<ByteArray>()
        val connectionManager2 = PeerConnectionManager(peerInfo2, packetConverter2)
        connectionManager2.registerBlockchain(blockchainRid, BlockchainDataReceiver(packets2))

        // Connecting
        connectionManager1.connectPeer(peerInfo2) { println("@1" + it.toString()) }
        connectionManager2.connectPeer(peerInfo1) { println("@2" + it.toString()) }

        // Sending packets
        connectionManager1.sendPacket(
                OutboundPacket(42, listOf(peerInfo2.pubKey.byteArrayKeyOf())))
        connectionManager1.sendPacket(
                OutboundPacket(42, listOf(peerInfo2.pubKey.byteArrayKeyOf())))

        // Then / Assert
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted {
                    assertArrayEquals(
                            arrayOf(
                                    byteArrayOf(0x02, 0x42, 0x42),
                                    byteArrayOf(0x02, 0x42, 0x42)
                            ),
                            packets2.toTypedArray())
                }

        // Then / Verify
        verify(packetConverter1)
        verify(packetConverter2)
    }

}