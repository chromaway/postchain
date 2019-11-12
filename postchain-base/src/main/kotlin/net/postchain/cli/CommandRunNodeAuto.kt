package net.postchain.cli

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import java.io.File
import java.nio.file.Paths

@Parameters(commandDescription = "Run Node Auto")
class CommandRunNodeAuto : Command {

    /**
     * Configuration directory structure:
     *
     *  config/
     *      node-config.properties
     *      blockchains/
     *          1/
     *              blockchain-rid
     *              0.conf.xml
     *              1.conf.xml
     *              ...
     *          2/
     *              ...
     */
    @Parameter(
            names = ["-d", "--directory"],
            description = "Configuration directory")
    private var configDirectory = "."

    private val NODE_CONFIG_FILE = "node-config.properties"
    private val BLOCKCHAIN_DIR = "blockchains"
    private val BLOCKCHAIN_RID_FILE = "brid.txt"

    override fun key(): String = "run-node-auto"

    override fun execute(): CliResult {
        println("run-auto-node will be executed with options: " +
                ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE))

        val chains = Paths.get(configDirectory, BLOCKCHAIN_DIR).toFile()
        val nodeConfigFile = Paths.get(configDirectory, NODE_CONFIG_FILE).toString()
        val chainIds = mutableListOf<Long>()
        val cliExecution = CliExecution()

        return try {
            if (chains.exists()) {
                cliExecution.waitDb(50, 1000, nodeConfigFile)
                chains.listFiles()
                        ?.filter(File::isDirectory)
                        ?.forEach { dir ->
                            val chainId = dir.name.toLong()
                            chainIds.add(chainId)
                            val brid = Paths.get(dir.absolutePath, BLOCKCHAIN_RID_FILE).toFile().readLines().first()

                            dir.listFiles()
                                    ?.filter { it.extension == "xml" }
                                    ?.forEach { file ->
                                        val blockchainConfigFile = file.absolutePath
                                        val height = (file.nameWithoutExtension.split(".")[0]).toLong()
                                        if (height.toInt() == 0) {
                                            cliExecution.addBlockchain(
                                                    nodeConfigFile, chainId, brid, blockchainConfigFile)
                                        } else {
                                            cliExecution.addConfiguration(
                                                    nodeConfigFile, blockchainConfigFile, chainId, height)
                                        }
                                    }
                        }
            }

            cliExecution.runNode(nodeConfigFile, chainIds.sorted())
            Ok("Postchain node launching is done", isLongRunning = true)

        } catch (e: CliError.Companion.CliException) {
            CliError.CommandNotAllowed(message = e.message)
        }
    }

}