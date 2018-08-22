package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.withWriteConnection
import net.postchain.baseStorage
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.gtx.encodeGTXValue
import net.postchain.gtx.gtxml.GTXMLValueParser
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import java.io.File

@Parameters(commandDescription = "Adds configuration")
class CommandAddConfiguration : Command {

    // TODO: Eliminate it later
    @Parameter(names = ["-nc", "--node-config"], description = "Configuration file of blockchain (.properties file)")
    private var nodeConfigFile = ""

    @Parameter(names = ["-a", "--archetype"], description = "Archetype of blockchain. Not currently used.")
    private var archetype = "base"

    @Parameter(names = ["-cid", "--chain-id"], description = "Id of blockchain", required = true)
    private var chainId = 0L

    @Parameter(names = ["-h", "--height"], description = "Height of configuration", required = true)
    private var height = 0L

    @Parameter(names = ["-bc", "--blockchain-config"], description = "Configuration file of blockchain (gtxml or binary)", required = true)
    private var blockchainConfigFile = ""

    override fun key(): String = "add-configuration"

    override fun execute() {
        println("add-configuration will be executed with options: " +
                "${ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE)}")

        val gtxValue = GTXMLValueParser.parseGTXMLValue(
                File(blockchainConfigFile).readText())
        val encodedGtxValue = encodeGTXValue(gtxValue)

        val nodeConfiguration = CommonsConfigurationFactory.readFromFile(nodeConfigFile)
        val storage = baseStorage(nodeConfiguration, -1 /*Will be eliminate later*/)

        var result = false
        withWriteConnection(storage, chainId) {
            result = BaseConfigurationDataStore
                    .addConfigurationData(it, height, encodedGtxValue) > 0
            true
        }

        println(reportMessage(result))
    }

    private fun reportMessage(result: Boolean): String {
        return if (result) "Configuration has been added successfully"
        else "Configuration has not been added"
    }
}