// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameters

@Parameters(commandDescription = "Configures node")
class CommandConfigureNode : Command {

    override fun key(): String = "configure-node"

    override fun execute(): CliResult {
        return CliError.NotImplemented(message = "configure-node command not implemented yet")
    }
}