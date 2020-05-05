// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.node

import assertk.assert
import assertk.assertions.isInstanceOf
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigurationProviderFactory.createProvider
import org.junit.Test

class NodeConfigurationProviderFactoryTest {

    @Test
    fun createLegacyProvider() {
        val appConfig: AppConfig = mock {
            on { nodeConfigProvider } doReturn "legacy"
        }

        assert(createProvider(appConfig)).isInstanceOf(
                LegacyNodeConfigurationProvider::class)
    }

    @Test
    fun createManualProvider() {
        val appConfig: AppConfig = mock {
            on { nodeConfigProvider } doReturn "Manual"
        }

        assert(createProvider(appConfig)).isInstanceOf(
                ManualNodeConfigurationProvider::class)
    }

    @Test
    fun createManagedProvider() {
        val appConfig: AppConfig = mock {
            on { nodeConfigProvider } doReturn "Managed"
        }

        assert(createProvider(appConfig)).isInstanceOf(
                ManagedNodeConfigurationProvider::class)
    }

    @Test
    fun createDefaultProvider() {
        val appConfig: AppConfig = mock {
            on { nodeConfigProvider } doReturn "some-unknown-provider-here"
        }

        assert(createProvider(appConfig)).isInstanceOf(
                LegacyNodeConfigurationProvider::class)
    }

    @Test
    fun createEmptyProvider() {
        val appConfig: AppConfig = mock {
            on { nodeConfigProvider } doReturn ""
        }

        assert(createProvider(appConfig)).isInstanceOf(
                LegacyNodeConfigurationProvider::class)
    }
}