package net.postchain.cli

import com.beust.jcommander.Parameters
import net.postchain.PostchainNode
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Stops node")
class CommandStopNode : Command {

    override fun key(): String = "stop-node"

    override fun execute() {
        println("stop-node will be executed with options: " +
                "${ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE)}")

        PostchainNode.stop()
    }
}