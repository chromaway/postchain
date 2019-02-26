package net.postchain.cli

import net.postchain.PostchainNode
import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.BaseBlockStore
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.common.hexStringToByteArray
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.gtx.encodeGTXValue
import net.postchain.gtx.gtxml.GTXMLValueParser
import java.io.File

class CliExecution {

    fun addBlockchain(nodeConfigFile: String, chainId: Long, blockchainRID: String, blockchainConfigFile: String, mode: AlreadyExistMode =  AlreadyExistMode.IGNORE) {
        val encodedGtxValue = getEncodedGtxValueFromFile(blockchainConfigFile)
        runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->

            fun init() {
                BaseBlockStore().initialize(ctx, blockchainRID.hexStringToByteArray())
                BaseConfigurationDataStore.addConfigurationData(ctx, 0, encodedGtxValue)
            }


            when (mode) {
                AlreadyExistMode.ERROR -> {
                    if (SQLDatabaseAccess().getBlockchainRID(ctx) == null) {
                        init()
                    } else {
                        throw CliError.Companion.CliException("Blockchain with chainId $chainId already exists. Use -f flag to force addition.")
                    }
                }

                AlreadyExistMode.FORCE -> {
                    init()
                }

                else -> {
                    if (SQLDatabaseAccess().getBlockchainRID(ctx) == null) {
                        init()
                    }
                }
            }
        }
    }

    fun addConfiguration(nodeConfigFile: String, blockchainConfigFile: String, chainId: Long, height: Long, mode: AlreadyExistMode = AlreadyExistMode.IGNORE) : Boolean {
        val encodedGtxValue = getEncodedGtxValueFromFile(blockchainConfigFile)
        var result = false
        runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->

            fun init() {
                result = BaseConfigurationDataStore.addConfigurationData(ctx, height, encodedGtxValue) > 0
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
        return result
    }

    fun runNode(nodeConfigFile: String, chainIDs: List<Long>) {
        val node = PostchainNode(CommonsConfigurationFactory.readFromFile(nodeConfigFile))
        chainIDs.forEach(node::startBlockchain)
    }

    private fun getEncodedGtxValueFromFile(blockchainConfigFile: String) :ByteArray {
        val gtxValue = GTXMLValueParser.parseGTXMLValue(File(blockchainConfigFile).readText())
        return encodeGTXValue(gtxValue)
    }
}