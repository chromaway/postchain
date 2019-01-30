package net.postchain.base

import net.postchain.common.hexStringToByteArray
import net.postchain.core.BlockchainContext
import net.postchain.core.NODE_ID_AUTO
import net.postchain.core.NODE_ID_READ_ONLY
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory
import org.apache.commons.configuration2.Configuration

class BaseBlockchainConfigurationData(
        val data: GtvDictionary,
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
        val stratDict = data["blockstrategy"] as GtvDictionary
        return stratDict?.get("name")?.asString() ?: ""
    }

    fun getBlockBuildingStrategy(): Gtv? {
        return data["blockstrategy"]
    }

    companion object {
        fun readFromCommonsConfiguration(config: Configuration, chainId: Long, blockchainRID: ByteArray, nodeID: Int):
                BaseBlockchainConfigurationData {

            val gtxConfig = convertConfigToGtv(config.subset("blockchain.$chainId")) as GtvDictionary
            val cryptoSystem = SECP256K1CryptoSystem()
            val privKey = gtxConfig["blocksigningprivkey"]!!.asByteArray()
            val pubKey = secp256k1_derivePubKey(privKey)
            val signer = cryptoSystem.makeSigner(pubKey, privKey) // TODO: maybe take it from somewhere?

            return BaseBlockchainConfigurationData(
                    gtxConfig,
                    BaseBlockchainContext(blockchainRID, nodeID, chainId, pubKey),
                    signer)
        }

        private fun convertGTXConfigToGtv(config: Configuration): Gtv {
            val properties = mutableListOf(
                    "modules" to GtvFactory.gtv(
                            config.getStringArray("gtx.modules").map { GtvFactory.gtv(it) }
                    )
            )

            if (config.containsKey("gtx.ft.assets")) {
                val ftProps = mutableListOf<Pair<String, Gtv>>()
                val assets = config.getStringArray("gtx.ft.assets")

                ftProps.add("assets" to GtvFactory.gtv(*assets.map { assetName ->
                    val issuers = GtvFactory.gtv(
                            *config.getStringArray("gtx.ft.asset.${assetName}.issuers").map(
                                    { GtvFactory.gtv(it.hexStringToByteArray()) }
                            ).toTypedArray())

                    GtvFactory.gtv(
                            "name" to GtvFactory.gtv(assetName),
                            "issuers" to issuers
                    )
                }.toTypedArray()))
                properties.add("ft" to GtvFactory.gtv(*ftProps.toTypedArray()))
            }

            if (config.containsKey("gtx.sqlmodules")) {
                properties.add("sqlmodules" to GtvFactory.gtv(*
                config.getStringArray("gtx.sqlmodules").map { GtvFactory.gtv(it) }.toTypedArray()
                ))
            }

            if (config.containsKey("gtx.rellSrcModule")) {
                properties.add("rellSrcModule" to GtvFactory.gtv(config.getString("gtx.rellSrcModule")))
            }

            return GtvFactory.gtv(*properties.toTypedArray())
        }

        private fun convertConfigToGtv(config: Configuration): Gtv {

            fun blockStrategy(config: Configuration): Gtv {
                return GtvFactory.gtv(
                        "name" to GtvFactory.gtv(config.getString("blockstrategy"))
                )
            }

            val properties = mutableListOf(
                    "blockstrategy" to blockStrategy(config),
                    "configurationfactory" to GtvFactory.gtv(config.getString("configurationfactory")),
                    "signers" to GtvFactory.gtv(config.getStringArray("signers").map { GtvFactory.gtv(it.hexStringToByteArray()) }),
                    "blocksigningprivkey" to GtvFactory.gtv(config.getString("blocksigningprivkey").hexStringToByteArray())
            )

            if (config.containsKey("gtx.modules")) {
                properties.add(Pair("gtx", convertGTXConfigToGtv(config)))
            }

            return GtvFactory.gtv(*properties.toTypedArray())
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
