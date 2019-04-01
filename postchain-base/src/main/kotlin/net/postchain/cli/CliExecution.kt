package net.postchain.cli

import net.postchain.PostchainNode
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.BaseBlockStore
import net.postchain.base.data.DatabaseAccess
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.gtx.encodeGTXValue
import net.postchain.gtx.gtxml.GTXMLValueParser
import org.apache.commons.configuration2.ex.ConfigurationException
import org.apache.commons.dbcp2.BasicDataSource
import java.io.File
import java.sql.Connection
import java.sql.SQLException

class CliExecution {

    fun addBlockchain(nodeConfigFile: String, chainId: Long, blockchainRID: String, blockchainConfigFile: String, mode: AlreadyExistMode =  AlreadyExistMode.IGNORE) {
        val encodedGtxValue = getEncodedGtxValueFromFile(blockchainConfigFile)

        runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->

            fun init() {
                BaseBlockStore().initialize(ctx, blockchainRID.hexStringToByteArray())
                BaseConfigurationDataStore.addConfigurationData(ctx, 0, encodedGtxValue)
            }

            val db = DatabaseAccess.of(ctx)

            when (mode) {
                AlreadyExistMode.ERROR -> {
                    if (db.getBlockchainRID(ctx) == null) {
                        init()
                    } else {
                        throw CliError.Companion.CliException("Blockchain with chainId $chainId already exists. Use -f flag to force addition.")
                    }
                }

                AlreadyExistMode.FORCE -> {
                    init()
                }

                else -> {
                    if (db.getBlockchainRID(ctx) == null) {
                        init()
                    }
                }
            }
        }
    }

    fun addConfiguration(nodeConfigFile: String, blockchainConfigFile: String, chainId: Long, height: Long, mode: AlreadyExistMode = AlreadyExistMode.IGNORE) {
        val encodedGtxValue = getEncodedGtxValueFromFile(blockchainConfigFile)
        runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->

            fun init() {
                BaseConfigurationDataStore.addConfigurationData(ctx, height, encodedGtxValue)
            }

            when (mode) {
                AlreadyExistMode.ERROR -> {
                    if (BaseConfigurationDataStore.getConfigurationData(ctx, height) == null) {
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
                    if (BaseConfigurationDataStore.getConfigurationData(ctx, height) == null) {
                        init()
                    } else {
                        println("Blockchain configuration of chainId $chainId at height $height already exists")
                    }
                }
            }
        }
    }

    fun runNode(nodeConfigFile: String, chainIDs: List<Long>) {
        val node = PostchainNode(CommonsConfigurationFactory.readFromFile(nodeConfigFile))
        chainIDs.forEach(node::startBlockchain)
    }

    fun checkBlockchain(nodeConfigFile: String, chainId: Long, blockchainRID: String) {
        runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->
            val chainIdBlockchainRid = DatabaseAccess.of(ctx).getBlockchainRID(ctx)
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
                else -> {
                }
            }
        }
    }

    fun waitDb(retryTimes: Int, retryInterval: Long, nodeConfigFile: String): CliResult {
        return tryCreateBasicDataSource(nodeConfigFile)?.let { Ok() }?:
        if(retryTimes > 0) {
            Thread.sleep(retryInterval)
            waitDb(retryTimes - 1, retryInterval, nodeConfigFile)
        } else CliError.DatabaseOffline()
    }

    private fun tryCreateBasicDataSource(nodeConfigFile: String): Connection? {
        return try {
            val config = CommonsConfigurationFactory.readFromFile(nodeConfigFile)
            BasicDataSource().apply {
                addConnectionProperty("currentSchema", config.getString("database.schema", "public"))
                driverClassName = config.getString("database.driverclass")
                url = "${config.getString("database.url")}?loggerLevel=OFF"
                username = config.getString("database.username")
                password = config.getString("database.password")
                defaultAutoCommit = false
            }.connection
        } catch (e: SQLException) {
            null
        } catch (e: ConfigurationException) {
            throw CliError.Companion.CliException("Failed to read configuration")
        }
    }

    private fun getEncodedGtxValueFromFile(blockchainConfigFile: String) :ByteArray {
        val gtxValue = GTXMLValueParser.parseGTXMLValue(File(blockchainConfigFile).readText())
        return encodeGTXValue(gtxValue)
    }
}