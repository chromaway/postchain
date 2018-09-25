package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.PostchainNode
import net.postchain.config.CommonsConfigurationFactory
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Runs node")
class CommandRunNode : Command {

    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of blockchain (.properties file)")
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-i", "--node-index"],
            description = "Node index")
    private var nodeIndex = 0

    @Parameter(
            names = ["-c", "--chain-ids"],
            required = true)
    private var chainIDs = listOf<Long>()

    override fun key(): String = "run-node"

    override fun execute() {
        println("run-node will be executed with options: " +
                "${ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE)}")

        nodeConfigFile = nodeConfigFile.takeIf { it != "" }
                ?: "config/config.$nodeIndex.properties"

        val node = PostchainNode(
                CommonsConfigurationFactory.readFromFile(nodeConfigFile))

        chainIDs.forEach(node::startBlockchain)
    }
}