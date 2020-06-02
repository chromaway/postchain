// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Adds configuration")
class CommandAddConfiguration : Command {

    // TODO: Eliminate it later or reduce to DbConfig only
    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of blockchain (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-i", "--infrastructure"],
            description = "The type of blockchain infrastructure. (Not currently used.)")
    private var infrastructureType = "base/ebft"

    @Parameter(
            names = ["-cid", "--chain-id"],
            description = "Id of blockchain",
            required = true)
    private var chainId = 0L

    @Parameter(
            names = ["-h", "--height"],
            description = "Height of configuration",
            required = true)
    private var height = 0L

    @Parameter(
            names = ["-bc", "--blockchain-config"],
            description = "Configuration file of blockchain (gtxml or binary)",
            required = true)
    private var blockchainConfigFile = ""

    @Parameter(
            names = ["-f", "--force"],
            description = "Force the addition of blockchain configuration " +
                    "which already exists of specified chain-id at height")
    private var force = false

    override fun key(): String = "add-configuration"

    override fun execute(): CliResult {
        println("add-configuration will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        return try {
            val mode = if (force) AlreadyExistMode.FORCE else AlreadyExistMode.ERROR
            CliExecution.addConfiguration(nodeConfigFile, blockchainConfigFile, chainId, height, mode)
            Ok("Configuration has been added successfully")
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

}