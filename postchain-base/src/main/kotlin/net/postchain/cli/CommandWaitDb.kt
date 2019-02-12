package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.config.CommonsConfigurationFactory
import org.apache.commons.configuration2.ex.ConfigurationException
import org.apache.commons.dbcp2.BasicDataSource
import java.lang.Thread.sleep
import java.sql.Connection
import java.sql.SQLException

@Parameters(commandDescription = "Block until successfully connected to db [ default retry times: 5, interval: 1000 millis ]")
class CommandWaitDb : Command {

    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of blockchain (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-rt", "--retry-times"],
            description = "Number of retries")
    private var retryTimes = 5

    @Parameter(
            names = ["-ri", "--retry-interval"],
            description = "Retry interval (millis)")
    private var retryInterval = 1000L

    override fun key(): String = "wait-db"

    override fun execute(): CliResult {
        return try {
            waitDb(retryTimes)
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

    private fun waitDb(retryTimes: Int): CliResult {
        return tryCreateBasicDataSource()?.let { Ok() }?:
        if(retryTimes > 0) {
            sleep(retryInterval)
            waitDb(retryTimes - 1)
        } else CliError.DatabaseOffline()
    }

    private fun tryCreateBasicDataSource(): Connection? {
        return try {
            val config = CommonsConfigurationFactory.readFromFile(nodeConfigFile)
            BasicDataSource().apply {
                addConnectionProperty("currentSchema", config.getString("database.schema", "public"))
                driverClassName = config.getString("database.driverclass")
                url = "${config.getString("database.url")}?loggerLevel=OFF"
                username = config.getString("database.username")
                password = config.getString("database.password")
                defaultAutoCommit = false
            }.connection
        } catch (e: SQLException) {
            null
        } catch (e: ConfigurationException) {
            throw CliError.Companion.CliException("Failed to read configuration")
        }
    }
}
