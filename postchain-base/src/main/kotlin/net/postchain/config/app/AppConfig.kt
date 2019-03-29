package net.postchain.config.app

import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler

class AppConfig(val config: Configuration) {

    /**
     * Configuration provider
     */
    val nodeConfigProvider: String
        get() = config.getString("configuration.provider.node") // legacy | manual | managed

    companion object {

        fun fromPropertiesFile(configFile: String): AppConfig {
            val params = Parameters().properties()
                    .setFileName(configFile)
                    .setListDelimiterHandler(DefaultListDelimiterHandler(','))

            val configuration = FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration::class.java)
                    .configure(params)
                    .configuration

            return AppConfig(configuration)
        }
    }
}