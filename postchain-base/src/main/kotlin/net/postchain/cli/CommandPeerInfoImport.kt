package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.PeerInfo
import net.postchain.common.toHex
import net.postchain.config.SimpleDatabaseConnector
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer
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

    fun peerinfoImport(nodeConfigFile: String): Array<PeerInfo> {
        val appConfig = AppConfig.fromPropertiesFile(nodeConfigFile)
        val nodeConfig = LegacyNodeConfigurationProvider(appConfig).getConfiguration()

        return if (nodeConfig.peerInfos.isEmpty()) {
            emptyArray()

        } else {
            SimpleDatabaseConnector(appConfig).withWriteConnection { connection ->
                val imported = mutableListOf<PeerInfo>()

                val dbLayer = AppConfigDbLayer(appConfig, connection)
                nodeConfig.peerInfos.forEach { peerInfo ->
                    val found = (dbLayer.findPeerInfo(peerInfo.host, peerInfo.port, null).isNotEmpty()
                            || dbLayer.findPeerInfo(null, null, peerInfo.pubKey.toHex()).isNotEmpty())
                    if (!found) {
                        val added = dbLayer.addPeerInfo(
                                peerInfo.host, peerInfo.port, peerInfo.pubKey.toHex(), peerInfo.createdAt, peerInfo.updatedAt)

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