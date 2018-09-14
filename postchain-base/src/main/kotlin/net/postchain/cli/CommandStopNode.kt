package net.postchain.cli

import com.beust.jcommander.Parameters

/**
 * Not currently used
 */
@Parameters(commandDescription = "Stops node")
class CommandStopNode : Command {

    override fun key(): String = "stop-node"

    override fun execute() {
        /* TODO: Uncomment at implementation
        println("stop-node will be executed with options: " +
                "${ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE)}")
                */
        println("stop-node command is not currently implemented")
    }
}