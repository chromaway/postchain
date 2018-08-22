package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Adds blockchain")
class CommandAddBlockchain : Command {

    // TODO: Eliminate it later
    @Parameter(names = ["-nc", "--node-config"], description = "Configuration file of blockchain (.properties file)")
    private var nodeConfigFile = ""

    @Parameter(names = ["-a", "--archetype"], description = "Archetype of blockchain. Not currently used.")
    private var archetype = "base"

    @Parameter(names = ["-cid", "--chain-id"], description = "Id of blockchain", required = true)
    private var chainId = 0L

    @Parameter(names = ["-bc", "--blockchain-config"], description = "Configuration file of blockchain (gtxml or binary)", required = true)
    private var blockchainConfigFile = ""

    override fun key(): String = "add-blockchain"

    override fun execute() {
        println("add-blockchain will be executed with options: " +
                "${ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE)}")

        println("add-blockchain command not implemented yet")
    }
}