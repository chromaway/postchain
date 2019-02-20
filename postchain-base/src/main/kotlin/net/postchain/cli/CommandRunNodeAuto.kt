package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Run Node Auto")
class CommandRunNodeAuto : Command {


    /**
     * Configuration directory example:
     * /config
        node-config.properties
        /blockchains
            /1
            blockchain-rid
            0.conf.xml
            100.conf.xml
     */
    @Parameter(
            names = ["-d", "--directory"],
            description = "Configuration directory")
    private var configDirectory = ""

    override fun key(): String = "run-node-auto"

    override fun execute(): CliResult {
        println("run-auto-node will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))
        return try {
            Ok("run auto node successfully")
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }
}