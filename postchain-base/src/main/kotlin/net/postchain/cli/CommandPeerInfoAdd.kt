// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.PeerInfo
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.runStorageCommand
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
                added -> Ok("Peerinfo has been added successfully")
                else -> Ok("Peerinfo hasn't been added")
            }
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

    private fun peerinfoAdd(nodeConfigFile: String, host: String, port: Int, pubKey: String, mode: AlreadyExistMode): Boolean {
        return runStorageCommand(nodeConfigFile) { ctx ->
            val db = DatabaseAccess.of(ctx)

            val found: Array<PeerInfo> = db.findPeerInfo(ctx, host, port, null)
            if (found.isNotEmpty()) {
                throw CliError.Companion.CliException("Peerinfo with port, host already exists.")
            }

            val found2 = db.findPeerInfo(ctx, null, null, pubKey)
            if (found2.isNotEmpty()) {
                when (mode) {
                    AlreadyExistMode.ERROR -> {
                        throw CliError.Companion.CliException("Peerinfo with pubkey already exists. Using -f to force update")
                    }
                    AlreadyExistMode.FORCE -> {
                        db.updatePeerInfo(ctx, host, port, pubKey)
                    }
                    else -> false
                }
            } else {
                when (mode) {
                    AlreadyExistMode.ERROR, AlreadyExistMode.FORCE -> {
                        db.addPeerInfo(ctx, host, port, pubKey)
                    }
                    else -> false
                }
            }
        }
    }
}