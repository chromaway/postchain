package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Find peerinfo")
class CommandPeerInfoFind : Command {

    // TODO: Eliminate it later or reduce to DbConfig only
    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of blockchain (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-h", "--host"],
            description = "Host")
    private var host: String? = null

    @Parameter(
            names = ["-p", "--port"],
            description = "Post")
    private var port: Int? = null

    @Parameter(
            names = ["-pk", "--pub-key"],
            description = "Public key")
    private var pubKey: String? = null

    override fun key(): String = "peerinfo-find"

    override fun execute(): CliResult {
        println("peerinfo-find will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        return try {
            val peerInfos = CliExecution().peerinfoFind(nodeConfigFile, host, port, pubKey)

            val report = if (peerInfos.isEmpty()) {
                "No peerinfo found"
            } else {
                peerInfos.mapIndexed(Templatter.PeerInfoTemplatter::renderPeerInfo)
                        .joinToString(
                                separator = "\n",
                                prefix = "Peerinfos (${peerInfos.size}):\n")
            }

            Ok(report)

        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }
}