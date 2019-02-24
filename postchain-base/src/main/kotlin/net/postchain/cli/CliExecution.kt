package net.postchain.cli

import net.postchain.base.BaseConfigurationDataStore
import net.postchain.base.data.BaseBlockStore
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.common.hexStringToByteArray
import net.postchain.gtx.encodeGTXValue
import net.postchain.gtx.gtxml.GTXMLValueParser
import java.io.File

class CliExecution {

    fun addBlockchain(nodeConfigFile: String, chainId: Long, blockchainRID: String, blockchainConfigFile: String, mode: AlreadyExistMode) {
        val encodedGtxValue = getEncodedGtxValueFromFile(blockchainConfigFile)
        runDBCommandBody(nodeConfigFile, chainId) { ctx, _ ->

            var init = {
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

    fun addConfiguration() {

    }

    fun runNode() {

    }

    private fun getEncodedGtxValueFromFile(blockchainConfigFile: String) :ByteArray {
        val gtxValue = GTXMLValueParser.parseGTXMLValue(File(blockchainConfigFile).readText())
        return encodeGTXValue(gtxValue)
    }
}