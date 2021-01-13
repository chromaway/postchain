// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.runStorageCommand
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Set this to ensure that chain is not split after a database loss.")
class CommandSetMustSyncUntil : Command {

    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of node (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-brid", "--blockchain-rid"],
            description = "Blockchain RID",
            required = true)
    private var blockchainRID = ""

    @Parameter(
            names = ["-h", "--height"],
            description = "Node must sync to this height before trying to build new blocks.",
            required = true)
    private var height = 0L

    override fun key(): String = "must-sync-until"

    override fun execute(): CliResult {
        println(key() + " will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        return try {
            val added = setMustSyncUntil(blockchainRID, height)
            return when {
                added -> Ok("Command " + key() + " finished successfully")
                else -> Ok("Blockchain replica already exists")
            }
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

    private fun setMustSyncUntil(blockchainRID: String, height: Long): Boolean {
        return runStorageCommand(nodeConfigFile) { ctx ->
            val db = DatabaseAccess.of(ctx)
            db.setMustSyncUntil(ctx, blockchainRID, height)
        }
    }

//    private fun addReplica(brid: String, pubKey: String): Boolean {
//        return runStorageCommand(nodeConfigFile) { ctx ->
//            val db = DatabaseAccess.of(ctx)
//
//            // Node must be in PeerInfo, or else it is not allowed as blockchain replica.
//            val foundInPeerInfo = db.findPeerInfo(ctx, null, null, pubKey)
//            if (foundInPeerInfo.isEmpty()) {
//                throw CliError.Companion.CliException("Given pubkey is not a peer. First add it as a peer.")
//            }
//
//            db.addBlockchainReplica(ctx, brid, pubKey)
//        }
//    }
}