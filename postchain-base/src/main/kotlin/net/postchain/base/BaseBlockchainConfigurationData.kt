package net.postchain.base

import net.postchain.common.hexStringToByteArray
import net.postchain.core.BlockchainContext
import net.postchain.core.NODE_ID_AUTO
import net.postchain.core.NODE_ID_READ_ONLY
import net.postchain.gtx.GTXValue
import net.postchain.gtx.gtx
import org.apache.commons.configuration2.Configuration

class BaseBlockchainConfigurationData(
        val data: GTXValue,
        partialContext: BlockchainContext,
        val blockSigner: Signer
) {

    val context: BlockchainContext
    val subjectID: ByteArray

    init {
        subjectID = partialContext.nodeRID!!

        context = BaseBlockchainContext(
                partialContext.blockchainRID,
                resolveNodeID(partialContext),
                partialContext.chainID,
                partialContext.nodeRID)
    }

    fun getSigners(): List<ByteArray> {
        return data["signers"]!!.asArray().map { it.asByteArray() }
    }


    fun getBlockBuildingStrategyName(): String {
        return data["blockstrategy"]?.get("name")?.asString() ?: ""
    }

    fun getBlockBuildingStrategy(): GTXValue? {
        return data["blockstrategy"]
    }

    companion object {
        fun readFromCommonsConfiguration(config: Configuration, chainId: Long, blockchainRID: ByteArray, nodeID: Int):
                BaseBlockchainConfigurationData {

            val gtxConfig = convertConfigToGTXValue(config.subset("blockchain.$chainId"))
            val cryptoSystem = SECP256K1CryptoSystem()
            val privKey = gtxConfig["blocksigningprivkey"]!!.asByteArray()
            val pubKey = secp256k1_derivePubKey(privKey)
            val signer = cryptoSystem.makeSigner(pubKey, privKey) // TODO: maybe take it from somewhere?

            return BaseBlockchainConfigurationData(
                    gtxConfig,
                    BaseBlockchainContext(blockchainRID, nodeID, chainId, pubKey),
                    signer)
        }

        private fun convertGTXConfigToGTXValue(config: Configuration): GTXValue {
            val properties = mutableListOf(
                    "modules" to gtx(
                            config.getStringArray("gtx.modules").map { gtx(it) }
                    )
            )

            if (config.containsKey("gtx.ft.assets")) {
                val ftProps = mutableListOf<Pair<String, GTXValue>>()
                val assets = config.getStringArray("gtx.ft.assets")

                ftProps.add("assets" to gtx(*assets.map { assetName ->
                    val issuers = gtx(
                            *config.getStringArray("gtx.ft.asset.${assetName}.issuers").map(
                                    { gtx(it.hexStringToByteArray()) }
                            ).toTypedArray())

                    gtx(
                            "name" to gtx(assetName),
                            "issuers" to issuers
                    )
                }.toTypedArray()))
                properties.add("ft" to gtx(*ftProps.toTypedArray()))
            }

            if (config.containsKey("gtx.sqlmodules"))
                properties.add("sqlmodules" to gtx(*
                config.getStringArray("gtx.sqlmodules").map { gtx(it) }.toTypedArray()
                ))

            if (config.containsKey("gtx.rellSrcModule"))
                properties.add("rellSrcModule" to gtx(config.getString("gtx.rellSrcModule")))

            return gtx(*properties.toTypedArray())
        }

        private fun convertConfigToGTXValue(config: Configuration): GTXValue {

            fun blockStrategy(config: Configuration): GTXValue {
                return gtx(
                        "name" to gtx(config.getString("blockstrategy"))
                )
            }

            val properties = mutableListOf(
                    "blockstrategy" to blockStrategy(config),
                    "configurationfactory" to gtx(config.getString("configurationfactory")),
                    "signers" to gtx(config.getStringArray("signers").map { gtx(it.hexStringToByteArray()) }),
                    "blocksigningprivkey" to gtx(config.getString("blocksigningprivkey").hexStringToByteArray())
            )

            if (config.containsKey("gtx.modules")) {
                properties.add(Pair("gtx", convertGTXConfigToGTXValue(config)))
            }

            return gtx(*properties.toTypedArray())
        }
    }

    private fun resolveNodeID(partialContext: BlockchainContext): Int {
        return if (partialContext.nodeID == NODE_ID_AUTO) {
            if (subjectID == null) {
                NODE_ID_READ_ONLY
            } else {
                getSigners()
                        .indexOfFirst { it.contentEquals(subjectID) }
                        .let { i -> if (i == -1) NODE_ID_READ_ONLY else i }
            }
        } else {
            partialContext.nodeID
        }
    }
}
