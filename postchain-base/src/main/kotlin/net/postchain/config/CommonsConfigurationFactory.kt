package net.postchain.config

import net.postchain.base.data.PostgreSQLCommands
import net.postchain.base.data.SAPHanaSQLCommands
import net.postchain.base.data.SQLCommands
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler

object CommonsConfigurationFactory {

    val POSTGRE_DRIVER_CLASS = "org.postgresql.Driver"
    val SAP_HANA_DRIVER_CLASS = "com.sap.db.jdbc.Driver"

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
            return SAPHanaSQLCommands
        }
    }
}