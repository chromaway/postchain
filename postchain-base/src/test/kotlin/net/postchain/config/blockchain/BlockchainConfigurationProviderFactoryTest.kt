package net.postchain.config.blockchain

import assertk.assertions.isInstanceOf
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import net.postchain.config.blockchain.BlockchainConfigurationProviderFactory.createProvider
import net.postchain.config.node.NodeConfig
import net.postchain.config.node.NodeConfigurationProvider
import org.junit.Test

class BlockchainConfigurationProviderFactoryTest {

    @Test
    fun createManualProvider() {
        val nodeConfig: NodeConfig = mock {
            on { blockchainConfigProvider } doReturn "legacy"
        }

        val nodeConfigProvider: NodeConfigurationProvider = mock {
            on { getConfiguration() } doReturn nodeConfig
        }

        assertk.assert(createProvider(nodeConfigProvider)).isInstanceOf(
                ManualBlockchainConfigurationProvider::class)
    }

    @Test
    fun createManagedProvider() {
        val nodeConfig: NodeConfig = mock {
            on { blockchainConfigProvider } doReturn "Managed"
        }

        val nodeConfigProvider: NodeConfigurationProvider = mock {
            on { getConfiguration() } doReturn nodeConfig
        }

        assertk.assert(createProvider(nodeConfigProvider)).isInstanceOf(
                ManagedBlockchainConfigurationProvider::class)
    }

    @Test
    fun createDefaultProvider() {
        val nodeConfig: NodeConfig = mock {
            on { blockchainConfigProvider } doReturn "some-unknown-provider-here"
        }

        val nodeConfigProvider: NodeConfigurationProvider = mock {
            on { getConfiguration() } doReturn nodeConfig
        }

        assertk.assert(createProvider(nodeConfigProvider)).isInstanceOf(
                ManualBlockchainConfigurationProvider::class)
    }

    @Test
    fun createEmptyProvider() {
        val nodeConfig: NodeConfig = mock {
            on { blockchainConfigProvider } doReturn ""
        }

        val nodeConfigProvider: NodeConfigurationProvider = mock {
            on { getConfiguration() } doReturn nodeConfig
        }

        assertk.assert(createProvider(nodeConfigProvider)).isInstanceOf(
                ManualBlockchainConfigurationProvider::class)
    }

}