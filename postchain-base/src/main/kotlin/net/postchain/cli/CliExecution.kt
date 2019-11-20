package net.postchain.cli

import net.postchain.PostchainNode
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.BlockchainRelatedInfo
import net.postchain.base.BlockchainRid
import net.postchain.base.data.BaseBlockStore
import net.postchain.base.data.DatabaseAccess
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.gtv.Gtv
import net.postchain.gtv.gtvml.GtvMLParser
import org.apache.commons.configuration2.ex.ConfigurationException
import org.apache.commons.dbcp2.BasicDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException

class CliExecution {

    /**
     * @return blockchain RID
     */
    fun addBlockchain(
            nodeConfigFile: String,
            chainId: Long,
            blockchainConfigFile: String,
            mode: AlreadyExistMode = AlreadyExistMode.IGNORE,
            givenDependencies: List<BlockchainRelatedInfo> = listOf()
    ): BlockchainRid {
        val gtvBcConf = getGtvFromFile(blockchainConfigFile)
        var bcRID: BlockchainRid? = null
        runDBCommandBody(nodeConfigFile, chainId) { eContext ->

            fun init(): BlockchainRid {
                val bcRid = BaseConfigurationDataStore.addConfigurationData(eContext, 0, gtvBcConf)
                BaseBlockStore().initialValidation(eContext, givenDependencies)
                return bcRid
            }

            val db = DatabaseAccess.of(eContext)

            bcRID = when (mode) {
                AlreadyExistMode.ERROR -> {
                    if (db.getBlockchainRID(eContext) == null) {
                        init()
                    } else {
                        throw CliError.Companion.CliException(
                                "Blockchain with chainId $chainId already exists. Use -f flag to force addition.")
                    }
                }

                AlreadyExistMode.FORCE -> {
                    init()
                }

                else -> {
                    db.getBlockchainRID(eContext) ?: init()
                }
            }
        }
        return bcRID!!
    }

    fun addConfiguration(
            nodeConfigFile: String,
            blockchainConfigFile: String,
            chainId: Long,
            height: Long,
            mode: AlreadyExistMode = AlreadyExistMode.IGNORE
    ) {

        val gtvBcConf = getGtvFromFile(blockchainConfigFile)
        runDBCommandBody(nodeConfigFile, chainId) { eContext ->

            fun init() {
                BaseConfigurationDataStore.addConfigurationData(eContext, height, gtvBcConf)
            }

            when (mode) {
                AlreadyExistMode.ERROR -> {
                    if (BaseConfigurationDataStore.getConfigurationData(eContext, height) == null) {
                        init()
                    } else {
                        throw CliError.Companion.CliException("Blockchain configuration of chainId $chainId at " +
                                "height $height already exists. Use -f flag to force addition.")
                    }
                }

                AlreadyExistMode.FORCE -> {
                    init()
                }

                else -> {
                    if (BaseConfigurationDataStore.getConfigurationData(eContext, height) == null) {
                        init()
                    } else {
                        println("Blockchain configuration of chainId $chainId at height $height already exists")
                    }
                }
            }
        }
    }

    fun runNode(nodeConfigFile: String, chainIDs: List<Long>) {
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(
                AppConfig.fromPropertiesFile(nodeConfigFile))

        with(PostchainNode(nodeConfigProvider)) {
            chainIDs.forEach { startBlockchain(it) }
        }
    }

    fun checkBlockchain(nodeConfigFile: String, chainId: Long, blockchainRID: String) {
        runDBCommandBody(nodeConfigFile, chainId) { eContext ->
            val chainIdBlockchainRid = DatabaseAccess.of(eContext).getBlockchainRID(eContext)
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
                BaseConfigurationDataStore.findConfigurationHeightForBlock(eContext, 0) == null -> {
                    throw CliError.Companion.CliException("No configuration found")
                }
                else -> {
                }
            }
        }
    }

    fun waitDb(retryTimes: Int, retryInterval: Long, nodeConfigFile: String): CliResult {
        return tryCreateBasicDataSource(nodeConfigFile)?.let { Ok() } ?: if (retryTimes > 0) {
            Thread.sleep(retryInterval)
            waitDb(retryTimes - 1, retryInterval, nodeConfigFile)
        } else CliError.DatabaseOffline()
    }

    private fun tryCreateBasicDataSource(nodeConfigFile: String): Connection? {
        return try {
            val nodeConfig = NodeConfigurationProviderFactory.createProvider(
                    AppConfig.fromPropertiesFile(nodeConfigFile)
            ).getConfiguration()

            BasicDataSource().apply {
                addConnectionProperty("currentSchema", nodeConfig.databaseSchema)
                driverClassName = nodeConfig.databaseDriverclass
                url = "${nodeConfig.databaseUrl}" //?loggerLevel=OFF"
                username = nodeConfig.databaseUsername
                password = nodeConfig.databasePassword
                defaultAutoCommit = false
            }.connection
        } catch (e: SQLException) {
            null
        } catch (e: ConfigurationException) {
            throw CliError.Companion.CliException("Failed to read configuration")
        }
    }

    private fun getGtvFromFile(blockchainConfigFile: String): Gtv {
        return GtvMLParser.parseGtvML(File(blockchainConfigFile).readText())
    }
}