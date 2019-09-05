package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.PeerInfo
import net.postchain.config.SimpleDatabaseConnector
import net.postchain.config.app.AppConfig
import net.postchain.config.app.AppConfigDbLayer
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Add peerinfo")
class CommandPeerInfoAdd : Command {

    // TODO: Eliminate it later or reduce to DbConfig only
    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of blockchain (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-h", "--host"],
            description = "Host",
            required = true)
    private var host = ""

    @Parameter(
            names = ["-p", "--port"],
            description = "Post",
            required = true)
    private var port = 0

    @Parameter(
            names = ["-pk", "--pub-key"],
            description = "Public key",
            required = true)
    private var pubKey = ""

    @Parameter(
            names = ["-f", "--force"],
            description = "Force the addition of peerinfo which already exists with the same host:port")
    private var force = false

    override fun key(): String = "peerinfo-add"

    override fun execute(): CliResult {
        println("peerinfo-add will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        return try {
            val mode = if (force) AlreadyExistMode.FORCE else AlreadyExistMode.ERROR
            val added = peerinfoAdd(nodeConfigFile, host, port, pubKey, mode)
            return when {
                added -> Ok("Peerinfo has been added")
                else -> Ok("Peerinfo hasn't been added")
            }
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

    fun peerinfoAdd(nodeConfigFile: String, host: String, port: Int, pubKey: String, mode: AlreadyExistMode): Boolean {
        val appConfig = AppConfig.fromPropertiesFile(nodeConfigFile)
        val connector = SimpleDatabaseConnector(appConfig)
        var peerinfos = arrayOf<PeerInfo>()

        connector.withReadConnection { connection ->
            peerinfos = AppConfigDbLayer(appConfig, connection).findPeerInfo(
                    host, port, null)
            if (!peerinfos.isEmpty()) {
                throw CliError.Companion.CliException("Peerinfo with port, host already exists.")
            }

            peerinfos = AppConfigDbLayer(appConfig, connection).findPeerInfo(
                    null, null, pubKey)
        }

        if (!peerinfos.isEmpty()) {
            return when (mode) {
                AlreadyExistMode.ERROR -> {
                    throw CliError.Companion.CliException("Peerinfo with pubkey already exists. Using -f to force update")
                }
                AlreadyExistMode.FORCE -> {
                    connector.withWriteConnection { connection ->
                        AppConfigDbLayer(appConfig, connection).updatePeerInfo(host, port, pubKey)
                    }
                }
                else -> false
            }
        } else {
            return when (mode) {
                AlreadyExistMode.ERROR, AlreadyExistMode.FORCE -> {
                    connector.withWriteConnection { connection ->
                        AppConfigDbLayer(appConfig, connection).addPeerInfo(host, port, pubKey)
                    }
                }
                else -> false
            }
        }
    }
}