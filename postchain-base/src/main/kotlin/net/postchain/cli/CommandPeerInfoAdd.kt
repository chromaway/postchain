// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.common.hexStringToByteArray
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Add peerinfo")
class CommandPeerInfoAdd : Command {

    // TODO: Eliminate it later or reduce to DbConfig only
    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of node (.properties file)",
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
        // Check that pubKey has length 66
        if (pubKey.length != 66) {
            return CliError.CommandNotAllowed(message = "Public key must have length 66")
        }

        //Check that pubkey can be parsed to a byteArray (holds exclusively values 0-9, a-f)
        try {
            pubKey.hexStringToByteArray()
        } catch (e: Exception) {
            return CliError.CommandNotAllowed(message = e.message + " Allowed characters in public keys are 0-9, a-f, A-F")
        }

        println("peerinfo-add will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        return try {
            val mode = if (force) AlreadyExistMode.FORCE else AlreadyExistMode.ERROR
            // Make all pubkey strings in db upper case. It will then be consistent with package net.postchain.common.
            //with HEX_CHARS = "0123456789ABCDEF"
            val added = CliExecution.peerinfoAdd(nodeConfigFile, host, port, pubKey.toUpperCase(), mode)
            when {
                added -> Ok("Peerinfo has been added successfully")
                else -> Ok("Peerinfo hasn't been added")
            }
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }
}