// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import net.postchain.PostchainNode
import net.postchain.base.*
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.data.DependenciesValidator
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.gtv.Gtv
import net.postchain.gtv.gtvml.GtvMLParser
import org.apache.commons.configuration2.ex.ConfigurationException
import org.apache.commons.dbcp2.BasicDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException

object CliExecution {

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

        val gtvData = parseGtvML(blockchainConfigFile)

        return runStorageCommand(nodeConfigFile, chainId) { ctx ->
            val db = DatabaseAccess.of(ctx)

            fun init(): BlockchainRid {
                val brid = BlockchainRidFactory.calculateBlockchainRid(gtvData)
                db.initializeBlockchain(ctx, brid)
                DependenciesValidator.validateBlockchainRids(ctx, givenDependencies)
                BaseConfigurationDataStore.addConfigurationData(ctx, 0, gtvData)
                return brid
            }

            when (mode) {
                AlreadyExistMode.ERROR -> {
                    if (db.getBlockchainRid(ctx) == null) {
                        init()
                    } else {
                        throw CliError.Companion.CliException(
                                "Blockchain with chainId $chainId already exists. Use -f flag to force addition.")
                    }
                }

                AlreadyExistMode.FORCE -> {
                    init()
                }

                AlreadyExistMode.IGNORE -> {
                    db.getBlockchainRid(ctx) ?: init()
                }
            }
        }
    }

    fun addConfiguration(
            nodeConfigFile: String,
            blockchainConfigFile: String,
            chainId: Long,
            height: Long,
            mode: AlreadyExistMode = AlreadyExistMode.IGNORE
    ) {

        val configStore = BaseConfigurationDataStore
        val gtvConfig = parseGtvML(blockchainConfigFile)

        runStorageCommand(nodeConfigFile, chainId) { ctx ->

            fun init() = configStore.addConfigurationData(ctx, height, gtvConfig)

            when (mode) {
                AlreadyExistMode.ERROR -> {
                    if (configStore.getConfigurationData(ctx, height) == null) {
                        init()
                    } else {
                        throw CliError.Companion.CliException("Blockchain configuration of chainId $chainId at " +
                                "height $height already exists. Use -f flag to force addition.")
                    }
                }

                AlreadyExistMode.FORCE -> {
                    init()
                }

                AlreadyExistMode.IGNORE -> {
                    if (configStore.getConfigurationData(ctx, height) == null) {
                        init()
                    } else {
                        println("Blockchain configuration of chainId $chainId at height $height already exists")
                    }
                }
            }
        }
    }

    fun runNode(nodeConfigFile: String, chainIds: List<Long>) {
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(
                AppConfig.fromPropertiesFile(nodeConfigFile))

        with(PostchainNode(nodeConfigProvider)) {
            chainIds.forEach { startBlockchain(it) }
        }
    }

    fun checkBlockchain(nodeConfigFile: String, chainId: Long, blockchainRID: String) {
        runStorageCommand(nodeConfigFile, chainId) { ctx ->
            val currentBrid = DatabaseAccess.of(ctx).getBlockchainRid(ctx)
            when {
                currentBrid == null -> {
                    throw CliError.Companion.CliException("Unknown chain-id: $chainId")
                }
                !blockchainRID.equals(currentBrid.toHex(), true) -> {
                    throw CliError.Companion.CliException("""
                        BlockchainRids are not equal:
                            expected: $blockchainRID
                            actual: ${currentBrid.toHex()}
                    """.trimIndent())
                }
                BaseConfigurationDataStore.findConfigurationHeightForBlock(ctx, 0) == null -> {
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

    private fun parseGtvML(blockchainConfigFile: String): Gtv {
        return GtvMLParser.parseGtvML(File(blockchainConfigFile).readText())
    }
}