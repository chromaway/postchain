// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.StorageBuilder
import net.postchain.config.app.AppConfig
import net.postchain.core.NODE_ID_NA

@Parameters(commandDescription = "Wipe db")
class CommandWipeDb : Command {

    override fun key(): String = "wipe-db"

    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of blockchain (.properties file)")
    private var nodeConfigFile = ""

    override fun execute(): CliResult {
        return try {
            val appConfig = AppConfig.fromPropertiesFile(nodeConfigFile)
            StorageBuilder.buildStorage(appConfig, NODE_ID_NA, true).close()
            Ok("Database has been wiped successfully")
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }
}