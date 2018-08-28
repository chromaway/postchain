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
)  {

    val context: BlockchainContext
    val subjectID: ByteArray

    init {
        subjectID = partialContext.nodeRID!!
        val nodeID: Int
        if (partialContext.nodeID == NODE_ID_AUTO) {
            if (subjectID == null) {
                nodeID = NODE_ID_READ_ONLY
            } else {
                val index = getSigners().indexOfFirst { it.contentEquals(subjectID) }
                if (index == -1) {
                    nodeID = NODE_ID_READ_ONLY
                } else {
                    nodeID = index
                }
            }
        } else {
            nodeID = partialContext.nodeID
        }
        context = BaseBlockchainContext(
                partialContext.blockchainRID,
                nodeID,
                partialContext.chainID,
                partialContext.nodeRID
        )
    }


    fun getSigners(): List<ByteArray> {
        return data["signers"]!!.asArray().map { it.asByteArray() }
    }

    fun getBlockBuildingStrategyName(): String {
        return data["blockstrategy"]?.get("name")?.asString() ?: ""
    }

    fun getBlockBuildingStrategy() : GTXValue? {
        return data["blockstrategy"]
    }

    companion object {
        fun readFromCommonsConfiguration(config: Configuration, chainID: Long, nodeID: Int): BaseBlockchainConfigurationData {
            val gConfig = convertConfigToGTXValue(config)
            val cryptoSystem = SECP256K1CryptoSystem()
            val privKey = gConfig["blocksigningprivkey"]!!.asByteArray()
            val pubKey = secp256k1_derivePubKey(privKey)
            val signer = cryptoSystem.makeSigner(
                pubKey, privKey // TODO: maybe take it from somewhere?
            )
            return BaseBlockchainConfigurationData(
                    gConfig,
                    BaseBlockchainContext(
                            gConfig["blockchainRID"]!!.asByteArray(),
                            nodeID,
                            chainID,
                            pubKey
                    ),
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

                ftProps.add("assets" to gtx(*assets.map {
                    assetName ->
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
                        config.getStringArray("gtx.sqlmodules").map {gtx(it)}.toTypedArray()
                ))

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
                    "blockchainRID" to gtx(config.getString("blockchainrid").hexStringToByteArray()),
                    "configurationfactory" to gtx(config.getString("configurationfactory")),
                    "signers" to gtx(config.getStringArray("signers").map { gtx(it.hexStringToByteArray())}),
                    "blocksigningprivkey" to gtx(config.getString("blocksigningprivkey").hexStringToByteArray())
            )

            if (config.containsKey("gtx.modules")) {
                properties.add(Pair("gtx", convertGTXConfigToGTXValue(config)))
            }

            return gtx(*properties.toTypedArray())
        }
    }
}
