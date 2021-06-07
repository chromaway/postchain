// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.BlockchainRid
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.runStorageCommand
import net.postchain.common.hexStringToByteArray
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Set this to ensure that chain is not split after a database loss.")
class CommandMustSyncUntil : Command {

    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of node (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-brid", "--blockchain-rid"],
            description = "ID for identifying a blockchain",
            required = true)
    private var bridString = ""

    @Parameter(
            names = ["-h", "--height"],
            description = "Node must sync to this height before trying to build new blocks.",
            required = true)
    private var height = 0L

    override fun key(): String = "must-sync-until"

    override fun execute(): CliResult {

        //Check that brid can be parsed to a byteArray (holds exclusively values 0-9, a-f, A-F)
        try {
            bridString.hexStringToByteArray()
        } catch (e: Exception) {
            return CliError.CommandNotAllowed(message = e.message + " Allowed characters in brid are 0-9, a-f, A-F")
        }

        println(key() + " will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        return try {
            val added = CliExecution.setMustSyncUntil(nodeConfigFile, BlockchainRid(bridString.hexStringToByteArray()),
                    height)
            return when {
                added -> Ok("Command " + key() + " finished successfully")
                else -> Ok("Command " + key() + " failed")
            }
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }
}