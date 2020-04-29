package net.postchain.devtools.utils.configuration

import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfig
import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.devtools.TestLegacyNodeConfigProducer
import org.apache.commons.configuration2.CompositeConfiguration
import org.apache.commons.configuration2.MapConfiguration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler
import java.io.File

/**
 * Will extract a [NodeConfigurationProvider] from a [NodeSetup].
 */
object NodeConfigurationProviderGenerator {

    /**
     * Builds a [NodeConfigurationProvider] from the [NodeSetup]
     *
     * @param testName is the name of the test
     * @param configOverrides is the configurations we always want
     * @param nodeSetup is the node we are working with
     * @param systemSetup is the architecture of the entire system we should test
     * @param setupAction is sometimes used to do an action on the setup
     */
    fun buildFromSetup(
            testName: String,
            configOverrides: MapConfiguration,
            nodeSetup: NodeSetup,
            systemSetup: SystemSetup,
            setupAction: (appConfig: AppConfig, nodeConfig: NodeConfig) -> Unit = { _, _ -> Unit }
    ): NodeConfigurationProvider {

        // TODO [Olle] I'm uncertain about this: could the Logic in TestLegacyNodeConfProducer be the same for managed mode too?
        val baseConfig = when (systemSetup.nodeConfProvider) {
            "legacy", "manual" -> TestLegacyNodeConfigProducer.createNodeConfig(testName, nodeSetup, systemSetup, null, configOverrides)
            "managed" -> throw IllegalArgumentException("Managed not implemented yet") // TODO [Olle] Implement
            else -> throw IllegalArgumentException("Don't know this provider")
        }
        return buildBase(baseConfig, configOverrides, setupAction)
    }

    /**
     * Transforms the [PropertiesConfiguration] -> [CompositeConfig] -> [AppConfig] and use the [NodeConfigurationProviderFactory] to build a real instance.
     *
     * @param baseConfig is the config we have built so far
     * @param configOverrides is the configurations we always want
     * @param setupAction is sometimes used to do an action on the setup
     * @return a conf provider where we have overidden the base config with the given overrides.
     */
    private fun buildBase(
            baseConfig: PropertiesConfiguration,
            configOverrides: MapConfiguration,
            setupAction: (appConfig: AppConfig, nodeConfig: NodeConfig) -> Unit = { _, _ -> Unit }
    ): NodeConfigurationProvider {
        val compositeConfig = CompositeConfiguration().apply {
            addConfiguration(configOverrides)
            addConfiguration(baseConfig)
        }

        val appConfig =AppConfig(compositeConfig)
        val provider = NodeConfigurationProviderFactory.createProvider(appConfig)

        // Run the action, default won't do anything
        setupAction(appConfig, provider.getConfiguration())

        return provider
    }

    /**
     * Sometimes we want to test that we can read the config file itself.
     * Note: this is not our usual procedure. Most tests will deduce the node configuration from a small set of fields.
     */
    fun readNodeConfFromFile(configFile: String): PropertiesConfiguration {
        // Read first file directly via the builder
        val params = Parameters()
                .fileBased()
//                .setLocationStrategy(ClasspathLocationStrategy())
                .setLocationStrategy(UniversalFileLocationStrategy())
                .setListDelimiterHandler(DefaultListDelimiterHandler(','))
                .setFile(File(configFile))

        return FileBasedConfigurationBuilder(PropertiesConfiguration::class.java)
                .configure(params)
                .configuration
    }
}