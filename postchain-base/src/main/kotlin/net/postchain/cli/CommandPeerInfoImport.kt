package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Import peerinfo")
class CommandPeerInfoImport : Command {

    // TODO: Eliminate it later or reduce to DbConfig only
    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of blockchain (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    override fun key(): String = "peerinfo-import"

    override fun execute(): CliResult {
        println("peerinfo-import will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        return try {
            val imported = CliExecution().peerinfoImport(nodeConfigFile)

            val report = if (imported.isEmpty()) {
                "No peerinfo have been imported"
            } else {
                imported.mapIndexed(Templatter.PeerInfoTemplatter::renderPeerInfo)
                        .joinToString(
                                separator = "\n",
                                prefix = "Peerinfo added (${imported.size}):\n")
            }

            Ok(report)

        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }
}