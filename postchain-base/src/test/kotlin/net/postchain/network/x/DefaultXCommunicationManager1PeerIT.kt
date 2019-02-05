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

    private fun startTestContext(peers: Array<PeerInfo>, myIndex: Int = 0): EbftIntegrationTestContext {
        val peerConfiguration = BasePeerCommConfiguration(
                peers, blockchainRid, myIndex, cryptoSystem, privKey)

        return EbftIntegrationTestContext(peerInfo, peerConfiguration)
    }

    @Test
    fun singlePeer_launched_successfully() {
        startTestContext(arrayOf(peerInfo))
                .use { context ->
                    context.communicationManager.init()

                    // Waiting for all connections establishing
                    await().atMost(Duration.FIVE_SECONDS)
                            .untilAsserted {
                                val actual = context.connectionManager.getConnectedPeers(context.chainId)
                                assert(actual).isEmpty()
                            }
                }
    }

    @Test(expected = IllegalArgumentException::class)
    fun singlePeer_launching_with_empty_peers_will_result_in_exception() {
        startTestContext(arrayOf())
                .use {
                    it.communicationManager.init()
                }
    }


    @Test(expected = IllegalArgumentException::class)
    fun singlePeer_launching_with_wrong_too_big_myIndex_will_result_in_exception() {
        startTestContext(arrayOf(peerInfo), 42)
                .use {
                    it.communicationManager.init()
                }
    }

    @Test(expected = IllegalArgumentException::class)
    fun singlePeer_launching_with_wrong_negative_myIndex_will_result_in_exception() {
        startTestContext(arrayOf(peerInfo), -1)
                .use {
                    it.communicationManager.init()
                }
    }

}