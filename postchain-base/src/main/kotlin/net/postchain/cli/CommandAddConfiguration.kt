package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.gtv.GtvEncoder.encodeGtv
import net.postchain.gtv.gtvml.GtvMLParser
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import java.io.File

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

    override fun execute() {
        println("add-configuration will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        val Gtv = GtvMLParser.parseGtvML(
                File(blockchainConfigFile).readText())
        val encodedGtv = encodeGtv(Gtv)

        var result = false
        runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->
            if (force || BaseConfigurationDataStore.getConfigurationData(ctx, height) == null) {
                result = BaseConfigurationDataStore.addConfigurationData(ctx, height, encodedGtv) > 0
            } else {
                println("Blockchain configuration of chainId $chainId at height $height " +
                        "already exists. Use -f flag to force addition.")
            }
        }
        println(reportMessage(result))
    }

    private fun reportMessage(result: Boolean): String {
        return if (result) "Configuration has been added successfully"
        else "Configuration has not been added"
    }
}