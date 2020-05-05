package net.postchain.common

import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler

object PropertiesFileLoader {

    fun load(filename: String): Configuration {
        // TODO: Or see https://www.mkyong.com/java/java-properties-file-examples/

        val params = Parameters().properties()
                .setFileName(filename)
                .setListDelimiterHandler(DefaultListDelimiterHandler(','))

        return FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration::class.java)
                .configure(params)
                .configuration
    }

}