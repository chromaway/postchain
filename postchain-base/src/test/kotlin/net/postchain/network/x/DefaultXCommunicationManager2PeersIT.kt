package net.postchain.network.x

import assertk.assert
import assertk.assertions.containsExactly
import net.postchain.base.BasePeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.secp256k1_derivePubKey
import net.postchain.core.byteArrayKeyOf
import net.postchain.ebft.message.GetBlockAtHeight
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.After
import org.junit.Before
import org.junit.Test

class DefaultXCommunicationManager2PeersIT {

    private val cryptoSystem = SECP256K1CryptoSystem()
    private val blockchainRid = ByteArray(64, Int::toByte)

    private lateinit var peerInfo1: PeerInfo
    private lateinit var peerInfo2: PeerInfo

    private lateinit var context1: EbftIntegrationTestContext
    private lateinit var context2: EbftIntegrationTestContext

    val privKey1 = cryptoSystem.getRandomBytes(32)
    val pubKey1 = secp256k1_derivePubKey(privKey1)

    val privKey2 = cryptoSystem.getRandomBytes(32)
    val pubKey2 = secp256k1_derivePubKey(privKey2)

    @Before
    fun setUp() {
        // TODO: [et]: Make dynamic ports
        peerInfo1 = PeerInfo("localhost", 3331, pubKey1)
        peerInfo2 = PeerInfo("localhost", 3332, pubKey2)
        val peers = arrayOf(peerInfo1, peerInfo2)

        // Creating
        context1 = EbftIntegrationTestContext(
                peerInfo1,
                BasePeerCommConfiguration(peers, blockchainRid, 0, cryptoSystem, privKey1))

        context2 = EbftIntegrationTestContext(
                peerInfo2,
                BasePeerCommConfiguration(peers, blockchainRid, 1, cryptoSystem, privKey2))

        // Initializing
        context1.communicationManager.init()
        context2.communicationManager.init()
    }

    @After
    fun tearDown() {
        context1.shutdown()
        context2.shutdown()
    }

    @Test
    fun twoPeers_SendsPackets_Successfully() {
        // Waiting for all connections establishing
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    val actual1 = context1.connectionManager.getConnectedPeers(context1.chainId)
                    assert(actual1).containsExactly(peerInfo2.pubKey.byteArrayKeyOf())

                    val actual2 = context2.connectionManager.getConnectedPeers(context2.chainId)
                    assert(actual2).containsExactly(peerInfo1.pubKey.byteArrayKeyOf())
                }

        // Sending packets
        // * 1 -> 2
        val packets1 = arrayOf(
                GetBlockAtHeight(10),
                GetBlockAtHeight(11))
        context1.communicationManager.sendPacket(packets1[0], XPeerID(pubKey2))
        context1.communicationManager.sendPacket(packets1[1], XPeerID(pubKey2))
        // * 2 -> 1
        val packets2 = arrayOf(
                GetBlockAtHeight(20),
                GetBlockAtHeight(21),
                GetBlockAtHeight(22))
        context2.communicationManager.sendPacket(packets2[0], XPeerID(pubKey1))
        context2.communicationManager.sendPacket(packets2[1], XPeerID(pubKey1))
        context2.communicationManager.sendPacket(packets2[2], XPeerID(pubKey1))

        // * asserting
        val actual1 = mutableListOf<Long>()
        val actual2 = mutableListOf<Long>()
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // Peer1
                    val actualPackets1 = context1.communicationManager.getPackets()
                    actual1.addAll(actualPackets1.map { (it.second as GetBlockAtHeight).height })
                    assert(actual1).containsExactly(20L, 21L, 22L)

                    // Peer2
                    val actualPackets2 = context2.communicationManager.getPackets()
                    actual2.addAll(actualPackets2.map { (it.second as GetBlockAtHeight).height })
                    assert(actual2).containsExactly(10L, 11L)
                }
    }
}