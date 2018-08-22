package net.postchain.config

import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler

object CommonsConfigurationFactory {

    fun readFromFile(configFile: String): Configuration {
        val params = Parameters().properties()
                .setFileName(configFile)
                .setListDelimiterHandler(DefaultListDelimiterHandler(','))

        return FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration::class.java)
                .configure(params)
                .configuration
    }
}