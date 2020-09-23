// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
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
            names = ["-cid", "--chain-ids"],
            description = "IDs of chains to launch",
            required = true)
    private var chainIDs = listOf<Long>()

    override fun key(): String = "run-node"

    override fun execute(): CliResult {
        println("run-node will be executed with options: " +
                "${ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE)}")

        nodeConfigFile = nodeConfigFile.takeIf { it != "" }
                ?: "config/config.$nodeIndex.properties"

        CliExecution.runNode(nodeConfigFile, chainIDs)
        return Ok("Postchain node is running", isLongRunning = true)
    }
}