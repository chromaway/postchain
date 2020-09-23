// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.PeerInfo
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.runStorageCommand
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Remove peerinfo")
class CommandPeerInfoRemove : Command {

    // TODO: Eliminate it later or reduce to DbConfig only
    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of blockchain (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-pk", "--pub-key"],
            description = "Public key",
            required = true)
    private var pubKey = ""

    override fun key(): String = "peerinfo-remove"

    override fun execute(): CliResult {
        println("peerinfo-remove will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        return try {
            val removed = peerinfoRemove(nodeConfigFile, pubKey)

            val report = if (removed.isEmpty()) {
                "No peerinfo has been removed"
            } else {
                removed.mapIndexed(Templater.PeerInfoTemplater::renderPeerInfo)
                        .joinToString(
                                separator = "\n",
                                prefix = "Peerinfo removed (${removed.size}):\n")
            }

            Ok(report)

        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

    private fun peerinfoRemove(nodeConfigFile: String, pubKey: String): Array<PeerInfo> {
        return runStorageCommand(nodeConfigFile) {
            DatabaseAccess.of(it).removePeerInfo(it, pubKey)
        }
    }
}