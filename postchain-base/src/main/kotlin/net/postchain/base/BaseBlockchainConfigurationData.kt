// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import net.postchain.common.hexStringToByteArray
import net.postchain.core.*
import net.postchain.gtv.*
import net.postchain.gtv.GtvFactory.gtv
import org.apache.commons.configuration2.Configuration

class BaseBlockchainConfigurationData(
        val data: GtvDictionary,
        partialContext: BlockchainContext,
        val blockSigMaker: SigMaker
) {

    val context: BlockchainContext
    val subjectID = partialContext.nodeRID!!

    init {
        context = BaseBlockchainContext(
                partialContext.blockchainRID,
                resolveNodeID(partialContext.nodeID),
                partialContext.chainID,
                partialContext.nodeRID)
    }

    fun getSigners(): List<ByteArray> {
        return data[KEY_SIGNERS]!!.asArray().map { it.asByteArray() }
    }

    fun getBlockBuildingStrategyName(): String {
        val stratDict = data[KEY_BLOCKSTRATEGY]
        return stratDict?.get(KEY_BLOCKSTRATEGY_NAME)?.asString() ?: ""
    }

    fun getHistoricBRID(): BlockchainRid? {
        val bytes = data[KEY_HISTORIC_BRID]?.asByteArray()
        return if (bytes != null)
            BlockchainRid(bytes)
        else
            null
    }

    fun getBlockBuildingStrategy(): Gtv? {
        return data[KEY_BLOCKSTRATEGY]
    }

    // default is 26 MiB
    fun getMaxBlockSize(): Long {
        val stratDict = data[KEY_BLOCKSTRATEGY]
        return stratDict?.get(KEY_BLOCKSTRATEGY_MAXBLOCKSIZE)?.asInteger() ?: 26 * 1024 * 1024
    }

    fun getMaxBlockTransactions(): Long {
        val stratDict = data[KEY_BLOCKSTRATEGY]
        return stratDict?.get(KEY_BLOCKSTRATEGY_MAXBLOCKTRANSACTIONS)?.asInteger() ?: 100
    }

    fun getDependenciesAsList(): List<BlockchainRelatedInfo> {
        val dep = data[KEY_DEPENDENCIES]
        return if (dep != null) {
            BaseDependencyFactory.build(dep!!)
        } else {
            // It is allowed to have no dependencies
            listOf<BlockchainRelatedInfo>()
        }
    }

    // default is 25 MiB
    fun getMaxTransactionSize(): Long {
        val gtxDict = data[KEY_GTX]
        return gtxDict?.get(KEY_GTX_TX_SIZE)?.asInteger() ?: 25 * 1024 * 1024
    }

    companion object {

        const val KEY_BLOCKSTRATEGY = "blockstrategy"
        const val KEY_BLOCKSTRATEGY_NAME = "name"
        const val KEY_BLOCKSTRATEGY_MAXBLOCKSIZE = "maxblocksize"
        const val KEY_BLOCKSTRATEGY_MAXBLOCKTRANSACTIONS = "maxblocktransactions"

        const val KEY_CONFIGURATIONFACTORY = "configurationfactory"

        const val KEY_SIGNERS = "signers"

        const val KEY_GTX = "gtx"
        const val KEY_GTX_MODULES = "modules"
        const val KEY_GTX_TX_SIZE = "max_transaction_size"

        const val KEY_DEPENDENCIES = "dependencies"

        const val KEY_HISTORIC_BRID = "historic_brid"


        @Deprecated("Deprecated in v2.4.4. Will be deleted in v3.0")
        private fun convertGTXConfigToGtv(config: Configuration): Gtv {
            val properties: MutableList<Pair<String, Gtv>> = mutableListOf(
                    "modules" to gtv(
                            config.getStringArray("gtx.modules").map { gtv(it) }
                    )
            )

            if (config.containsKey("gtx.ft.assets")) {
                val ftProps = mutableListOf<Pair<String, Gtv>>()
                val assets = config.getStringArray("gtx.ft.assets")

                ftProps.add("assets" to gtv(*assets.map { assetName ->
                    val issuers = gtv(
                            *config.getStringArray("gtx.ft.asset.$assetName.issuers").map(
                                    { gtv(it.hexStringToByteArray()) }
                            ).toTypedArray())

                    gtv(
                            "name" to gtv(assetName),
                            "issuers" to issuers
                    )
                }.toTypedArray()))
                properties.add("ft" to gtv(*ftProps.toTypedArray()))
            }

            if (config.containsKey("gtx.sqlmodules")) {
                properties.add("sqlmodules" to gtv(*
                config.getStringArray("gtx.sqlmodules").map { gtv(it) }.toTypedArray()
                ))
            }

            if (config.containsKey("gtx.rellSrcModule")) {
                properties.add("rellSrcModule" to gtv(config.getString("gtx.rellSrcModule")))
            }

            return gtv(*properties.toTypedArray())
        }
    }

    private fun resolveNodeID(nodeID: Int): Int {
        return if (nodeID == NODE_ID_AUTO) {
            if (subjectID == null) {
                NODE_ID_READ_ONLY
            } else {
                getSigners()
                        .indexOfFirst { it.contentEquals(subjectID) }
                        .let { i -> if (i == -1) NODE_ID_READ_ONLY else i }
            }
        } else {
            nodeID
        }
    }
}
