package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.PostchainNode
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Runs node")
class CommandRunNode : Command {

    @Parameter(names = ["-i", "--nodeIndex"], description = "Node index")
    private var nodeIndex = 0

    @Parameter(names = ["-c", "--config"], description = "Configuration file of blockchain (.properties file)")
    private var configFile = ""

    override fun key(): String = "run-node"

    override fun execute() {
        println("run-node will be executed with options: " +
                "${ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE)}")

        if (configFile == "") {
            configFile = "config/config.$nodeIndex.properties"
        }

        PostchainNode.start(configFile, nodeIndex)
    }
}