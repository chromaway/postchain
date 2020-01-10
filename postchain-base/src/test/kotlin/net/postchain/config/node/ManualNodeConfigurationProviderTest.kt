package net.postchain.config.node

import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import net.postchain.base.PeerInfo
import net.postchain.base.peerId
import net.postchain.common.hexStringToByteArray
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer
import org.junit.Test
import java.time.Instant

class ManualNodeConfigurationProviderTest {

    private val peerInfo0 = PeerInfo("127.0.0.1", 9900, "AAAA".hexStringToByteArray(), Instant.EPOCH)
    private val peerInfo1 = PeerInfo("127.0.0.1", 9901, "BBBB".hexStringToByteArray(), Instant.EPOCH)

    @Test
    fun testGetConfiguration() {
        // Expected
        val expected = arrayOf(peerInfo0, peerInfo1)
        val actual = mapOf(
                peerInfo0.peerId() to peerInfo0,
                peerInfo1.peerId() to peerInfo1)

        // Mock
        val appConfig = AppConfig(mock())
        val mockAppConfigDbLayer: AppConfigDbLayer = mock {
            on { getPeerInfoCollection() } doReturn expected
        }

        // SUT
        val provider = ManualNodeConfigurationProvider(
                appConfig, { MockDatabaseConnector() }, { _, _ -> mockAppConfigDbLayer })

        // Assert
        val config = provider.getConfiguration()
        assertk.assert(config.appConfig).isSameAs(appConfig)
        assertk.assert(config.peerInfoMap).isEqualTo(actual)
    }

    @Test
    fun testGetPeerInfoCollection() {
        // Expected
        val expected = arrayOf(peerInfo1, peerInfo0)
        val actual = arrayOf(peerInfo1, peerInfo0)

        // Mock
        val mockAppConfigDbLayer: AppConfigDbLayer = mock {
            on { getPeerInfoCollection() } doReturn expected
        }

        // SUT
        val provider = ManualNodeConfigurationProvider(
                mock(), { MockDatabaseConnector() }, { _, _ -> mockAppConfigDbLayer })

        // Assert
        val peerInfos = provider.getPeerInfoCollection(mock())
        assertk.assert(peerInfos).containsExactly(*actual)
    }
}