// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

@Parameters(commandDescription = "Block until successfully connected to db [ default retry times: 5, interval: 1000 millis ]")
class CommandWaitDb : Command {

    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of node (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-rt", "--retry-times"],
            description = "Number of retries")
    private var retryTimes = 50

    @Parameter(
            names = ["-ri", "--retry-interval"],
            description = "Retry interval (millis)")
    private var retryInterval = 1000L

    override fun key(): String = "wait-db"

    override fun execute(): CliResult {
        return try {
            CliExecution.waitDb(retryTimes, retryInterval, nodeConfigFile)
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }


}
