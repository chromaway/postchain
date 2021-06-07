package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.BlockchainRid
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.runStorageCommand
import net.postchain.network.x.XPeerID
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle


@Parameters(commandDescription = "Remove node as replica for given blockchain rid. If brid not given command will be " +
        "applied on all blockchains.")
class CommandBlockchainReplicaRemove : Command {
    // TODO: Eliminate it later or reduce to DbConfig only
    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of node (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-brid", "--blockchain-rid"],
            description = "Blockchain RID",
            required = false)
    private var blockchainRID = ""

    @Parameter(
            names = ["-pk", "--pub-key"],
            description = "Public key",
            required = true)
    private var pubKey = ""

    override fun key(): String = "blockchain-replica-remove"

    override fun execute(): CliResult {
        println("blockchain-replica-remove will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        return try {
            val brid = if (blockchainRID == "") null else blockchainRID
            val removed = blockchainReplicaRemove(brid, pubKey)

            val report = if (removed.isEmpty()) {
                "No replica has been removed"
            } else {
                val listRemoved = removed.map { it.toString() }
                listRemoved.joinToString(
                                separator = "\n",
                                prefix = "Replica $pubKey removed from brid (${removed.size}):\n")
            }

            Ok(report)

        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

    private fun blockchainReplicaRemove(brid: String?, pubKey: String): Set<BlockchainRid> {
        return runStorageCommand(nodeConfigFile) {
            DatabaseAccess.of(it).removeBlockchainReplica(it, brid, pubKey)
        }
    }
}
