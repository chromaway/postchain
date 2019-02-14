package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.common.toHex
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@Parameters(commandDescription = "Checks blockchain")
class CommandCheckBlockchain : Command {

    private val dbAccess = SQLDatabaseAccess()

    // TODO: Eliminate it later or reduce to DbConfig only
    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of blockchain (.properties file)",
            required = true)
    private var nodeConfigFile = ""

    /*
    @Parameter(
            names = ["-i", "--infrastructure"],
            description = "The type of blockchain infrastructure. (Not currently used.)")
    private var infrastructureType = "base/ebft"
    */

    @Parameter(
            names = ["-cid", "--chain-id"],
            description = "Local number id of blockchain",
            required = true)
    private var chainId = 0L

    @Parameter(
            names = ["-brid", "--blockchain-rid"],
            description = "Blockchain global ID",
            required = true)
    private var blockchainRID = ""

    override fun key(): String = "check-blockchain"

    override fun execute(): CliResult {
        println("check-blockchain will be executed with options: " +
                toStringExclude(this, "dbAccess", ToStringStyle.SHORT_PREFIX_STYLE))

        return try {
            runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->
                val chainIdBlockchainRid = dbAccess.getBlockchainRID(ctx)
                when {
                    chainIdBlockchainRid == null -> {
                        throw CliError.Companion.CliException("Unknown chain-id: $chainId")
                    }
                    !blockchainRID.equals(chainIdBlockchainRid.toHex(), true) -> {
                        throw CliError.Companion.CliException("""
                            BlockchainRids are not equal:
                                expected: $blockchainRID
                                actual: ${chainIdBlockchainRid.toHex()}
                        """.trimIndent())
                    }
                    BaseConfigurationDataStore.findConfiguration(ctx, 0) == null -> {
                        throw CliError.Companion.CliException("No configuration found")
                    }
                    else -> {}
                }
            }
            Ok("Okay")
        } catch (e: CliError.Companion.CliException) {
            CliError.CheckBlockChain(message = e.message)
        }
    }

    private fun toStringExclude(obj: Any, excludeField: String, style: ToStringStyle): String {
        return object : ReflectionToStringBuilder(obj, style) {
            override fun accept(field: java.lang.reflect.Field): Boolean {
                return field.name != excludeField
            }
        }.build()
    }
}