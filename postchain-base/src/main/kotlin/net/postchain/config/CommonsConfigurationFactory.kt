package net.postchain.config

import net.postchain.base.data.SQLCommands
import net.postchain.base.data.postgresql.PostgreSQLCommands
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler

object CommonsConfigurationFactory {

    val POSTGRE_DRIVER_CLASS = "database.driverclass"

    fun readFromFile(configFile: String): Configuration {
        val params = Parameters().properties()
                .setFileName(configFile)
                .setListDelimiterHandler(DefaultListDelimiterHandler(','))

        return FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration::class.java)
                .configure(params)
                .configuration
    }

    fun getSQLCommandsImplementation(driverClassName : String) : SQLCommands {
        if (driverClassName == POSTGRE_DRIVER_CLASS) {
            return PostgreSQLCommands
        } else {
            return PostgreSQLCommands
        }
    }
}