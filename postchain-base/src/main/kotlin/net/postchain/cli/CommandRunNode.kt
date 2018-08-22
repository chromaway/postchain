package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.PostchainNode
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Runs node")
class CommandRunNode : Command {

    @Parameter(names = ["-c", "--node-config"], description = "Configuration file of blockchain (.properties file)")
    private var nodeConfigFile = ""

    @Parameter(names = ["-i", "--node-index"], description = "Node index")
    private var nodeIndex = 0

    override fun key(): String = "run-node"

    override fun execute() {
        println("run-node will be executed with options: " +
                "${ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE)}")

        if (nodeConfigFile == "") {
            nodeConfigFile = "config/config.$nodeIndex.properties"
        }

        PostchainNode().start(nodeConfigFile, nodeIndex)
    }
}