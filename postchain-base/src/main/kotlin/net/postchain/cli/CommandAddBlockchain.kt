package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.BaseBlockStore
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvEncoder.encodeGtv
import net.postchain.gtv.gtvml.GtvMLParser
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import java.io.File

@Parameters(commandDescription = "Adds blockchain")
class CommandAddBlockchain : Command {

    // TODO: Eliminate it later or reduce to DbConfig only
    @Parameter(
            names = ["-nc", "--node-config"],
            description = "Configuration file of blockchain (.properties file)")
    private var nodeConfigFile = ""

    @Parameter(
            names = ["-i", "--infrastructure"],
            description = "The type of blockchain infrastructure.")
    private var infrastructureType = "base/ebft"

    @Parameter(
            names = ["-cid", "--chain-id"],
            description = "Local number id of blockchain",
            required = true)
    private var chainId = 0L

    @Parameter(
            names = ["-brid", "--blockchain-rid"],
            description = "Blockchain global ID",
            required = true)
    private var blockchainRID: String = ""

    @Parameter(
            names = ["-bc", "--blockchain-config"],
            description = "Configuration file of blockchain (gtxml or binary)",
            required = true)
    private var blockchainConfigFile = ""

    @Parameter(
            names = ["-f", "--force"],
            description = "Force the addition of already existed blockchain-rid (by chain-id)")
    private var force = false

    override fun key(): String = "add-blockchain"

    override fun execute() {
        println("add-blockchain will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        val Gtv = GtvMLParser.parseGtvML(
                File(blockchainConfigFile).readText())
        val encodedGtv = encodeGtv(Gtv)

        runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->
            if (force || SQLDatabaseAccess().getBlockchainRID(ctx) == null) {
                BaseBlockStore().initialize(ctx, blockchainRID.hexStringToByteArray())
                BaseConfigurationDataStore.addConfigurationData(ctx, 0, encodedGtv)
                println("Configuration has been added successfully")
            } else {
                println("Blockchain with chainId $chainId already exists. Use -f flag to force addition.")
            }
        }
    }
}