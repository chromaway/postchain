package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import net.postchain.PostchainNode
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.BaseBlockStore
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.common.hexStringToByteArray
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.gtx.encodeGTXValue
import net.postchain.gtx.gtxml.GTXMLValueParser
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import java.io.File

@Parameters(commandDescription = "Run Node Auto")
class CommandRunNodeAuto : Command {


    /**
     * Configuration directory example:
     * /config
        node-config.properties
        /blockchains
            /1
            blockchain-rid
            0.conf.xml
            100.conf.xml
     */
    @Parameter(
            names = ["-d", "--directory"],
            description = "Configuration directory")
    private var configDirectory = ""

    private val NODE_CONFIG_FILE = "node-config.properties"
    private val BLOCKCHAIN_DIR = "blockchains"
    private val BLOCKCHAIN_RID_FILE = "brid.txt"

    override fun key(): String = "run-node-auto"

    override fun execute(): CliResult {
        println("run-auto-node will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        val path = configDirectory + File.separator + BLOCKCHAIN_DIR
        val nodeConfigFile = configDirectory + File.separator + NODE_CONFIG_FILE
        var chainIds = mutableListOf<Long>()

        return try {
            File(path).listFiles().forEach {
                if (it.isDirectory) {
                    val chainId = it.name.toLong()
                    chainIds.add(chainId)
                    val brid = File(it.absolutePath + File.separator + BLOCKCHAIN_RID_FILE).readText()

                    it.listFiles().forEach {
                        if (it.extension == "xml") {
                            val blockchainConfigFile = it.absolutePath;
                            var height = (it.nameWithoutExtension.split(".")[0]).toLong()
                            if (height.toInt() == 0) {
                                addBlockChain(nodeConfigFile, blockchainConfigFile, chainId, brid)
                            } else {
                                addConfiguration(nodeConfigFile, blockchainConfigFile, chainId, brid, height)
                            }
                        }
                    }

                }
            }
            runNode(nodeConfigFile, chainIds)
            Ok("Postchain node launching is done", isLongRunning = true)
        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

    private fun addBlockChain(nodeConfigFile: String, blockchainConfigFile: String, chainId: Long, blockchainRID: String) {
        val gtxValue = GTXMLValueParser.parseGTXMLValue(File(blockchainConfigFile).readText())
        val encodedGtxValue = encodeGTXValue(gtxValue)
        runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->
            if (SQLDatabaseAccess().getBlockchainRID(ctx) == null) {
                BaseBlockStore().initialize(ctx, blockchainRID.hexStringToByteArray())
                BaseConfigurationDataStore.addConfigurationData(ctx, 0, encodedGtxValue)
            } else {
                println("Blockchain with chainId $chainId already exists")
            }
        }

    }

    private fun addConfiguration(nodeConfigFile: String, blockchainConfigFile: String, chainId: Long, blockchainRID: String, height: Long) {
        val gtxValue = GTXMLValueParser.parseGTXMLValue(File(blockchainConfigFile).readText())
        val encodedGtxValue = encodeGTXValue(gtxValue)
        runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->
            if (BaseConfigurationDataStore.getConfigurationData(ctx, height) == null) {
                BaseConfigurationDataStore.addConfigurationData(ctx, height, encodedGtxValue)
            } else {
                println("Blockchain configuration of chainId $chainId at " + "height $height already exists")
            }
        }

    }

    private fun runNode(nodeConfigFile: String, chainIDs: List<Long>) {
        val node = PostchainNode(CommonsConfigurationFactory.readFromFile(nodeConfigFile))
        chainIDs.forEach(node::startBlockchain)
    }

}