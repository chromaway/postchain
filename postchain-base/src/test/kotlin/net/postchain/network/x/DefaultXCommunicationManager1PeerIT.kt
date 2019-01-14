package net.postchain.network.x

import assertk.assert
import assertk.assertions.isEmpty
import net.postchain.base.BasePeerCommConfiguration
import net.postchain.base.PeerInfo
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.base.secp256k1_derivePubKey
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.Test

class DefaultXCommunicationManager1PeerIT {

    private val cryptoSystem = SECP256K1CryptoSystem()
    private val blockchainRid = ByteArray(64, Int::toByte)

    private val privKey = cryptoSystem.getRandomBytes(32)
    private val pubKey = secp256k1_derivePubKey(privKey)
    private val peerInfo = PeerInfo("localhost", 3331, pubKey)

    @Test
    fun singlePeer_launched_successfully() {
        val context = buildTestContext(arrayOf(peerInfo))

        // Waiting for all connections establishing
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    val actual = context.connectionManager.getConnectedPeers(context.chainId)
                    assert(actual).isEmpty()
                }

        context.shutdown()
    }

    @Test
    fun singlePeer_launched_with_empty_peers_config_successfully() {
        val context = buildTestContext(arrayOf())

        // Waiting for all connections establishing
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    val actual = context.connectionManager.getConnectedPeers(context.chainId)
                    assert(actual).isEmpty()
                }

        context.shutdown()
    }

    /*
    @Test
    fun singlePeer_launched_with_wrong_myIndex_in_config_successfully() {
        val context = buildTestContext(arrayOf(peerInfo), 42)

        // Waiting for all connections establishing
        await().atMost(Duration.FIVE_SECONDS)
                .untilAsserted {
                    val actual1 = context.connectionManager.getConnectedPeers(context.chainId)
                    assert(actual1).isEmpty()
                }

        context.shutdown()
    }
    */

    private fun buildTestContext(peers: Array<PeerInfo>, myIndex: Int = 0): EbftIntegrationTestContext {
        val peerConfiguration = BasePeerCommConfiguration(peers, blockchainRid, myIndex, cryptoSystem, privKey)
        return EbftIntegrationTestContext(peerInfo, peerConfiguration)
                .apply { communicationManager.init() }
    }
}