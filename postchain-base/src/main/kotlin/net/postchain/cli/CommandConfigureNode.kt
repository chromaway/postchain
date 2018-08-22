package net.postchain.cli

import com.beust.jcommander.Parameters

@Parameters(commandDescription = "Configures node")
class CommandConfigureNode : Command {

    override fun key(): String = "configure-node"

    override fun execute() {
        println("configure-node command not implemented yet")
    }
}