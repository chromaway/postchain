// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.PeerInfo
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.runStorageCommand
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Find peerinfo")
class CommandPeerInfoFind : Command {

    // TODO: Eliminate it later or reduce to DbConfig only
    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of node (.properties file)",
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
            val peerInfos = peerinfoFind(nodeConfigFile, host, port, pubKey)

            val report = if (peerInfos.isEmpty()) {
                "No peerinfo found"
            } else {
                peerInfos.mapIndexed(Templater.PeerInfoTemplater::renderPeerInfo)
                        .joinToString(
                                separator = "\n",
                                prefix = "Peerinfos (${peerInfos.size}):\n")
            }

            Ok(report)

        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

    private fun peerinfoFind(nodeConfigFile: String, host: String?, port: Int?, pubKey: String?): Array<PeerInfo> {
        return runStorageCommand(nodeConfigFile) {
            DatabaseAccess.of(it).findPeerInfo(it, host, port, pubKey)
        }
    }
}