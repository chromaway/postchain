// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.PeerInfo
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.runStorageCommand
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.config.node.LegacyNodeConfigurationProvider
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
            val imported = peerinfoImport(nodeConfigFile)

            val report = if (imported.isEmpty()) {
                "No peerinfo have been imported"
            } else {
                imported.mapIndexed(Templater.PeerInfoTemplater::renderPeerInfo)
                        .joinToString(
                                separator = "\n",
                                prefix = "Peerinfo added (${imported.size}):\n")
            }

            Ok(report)

        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

    private fun peerinfoImport(nodeConfigFile: String): Array<PeerInfo> {
        val appConfig = AppConfig.fromPropertiesFile(nodeConfigFile)
        val nodeConfig = LegacyNodeConfigurationProvider(appConfig).getConfiguration()

        return if (nodeConfig.peerInfoMap.isEmpty()) {
            emptyArray()
        } else {
            runStorageCommand(nodeConfigFile) { ctx ->
                val db = DatabaseAccess.of(ctx)
                val imported = mutableListOf<PeerInfo>()

                nodeConfig.peerInfoMap.values.forEach { peerInfo ->
                    val noHostPort = db.findPeerInfo(ctx, peerInfo.host, peerInfo.port, null).isEmpty()
                    val noPubKey = db.findPeerInfo(ctx, null, null, peerInfo.pubKey.toHex()).isEmpty()

                    if (noHostPort && noPubKey) {
                        val added = db.addPeerInfo(
                                ctx, peerInfo.host, peerInfo.port, peerInfo.pubKey.toHex(), peerInfo.timestamp)

                        if (added) {
                            imported.add(peerInfo)
                        }
                    }
                }

                imported.toTypedArray()
            }
        }
    }
}