package net.postchain.base

import net.postchain.common.hexStringToByteArray
import net.postchain.core.*
import net.postchain.gtv.*
import net.postchain.gtv.GtvDictionary
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
                resolveNodeID(partialContext),
                partialContext.chainID,
                partialContext.nodeRID)
    }

    fun getSigners(): List<ByteArray> {
        return data["signers"]!!.asArray().map { it.asByteArray() }
    }

    fun getBlockBuildingStrategyName(): String {
        val stratDict = data["blockstrategy"]
        return stratDict?.get("name")?.asString() ?: ""
    }

    fun getBlockBuildingStrategy(): Gtv? {
        return data["blockstrategy"]
    }

    fun getDependenciesAsList(): List<BlockchainRelatedInfo> {
        val dep = data["dependencies"]
        return if (dep != null) {
            try {
                // Should contain an array of String, ByteArr pairs
                val gtvDepArray = dep!! as GtvArray
                val depList = mutableListOf<BlockchainRelatedInfo>()
                for (element in gtvDepArray.array) {
                    val elemArr = element as GtvArray
                    val nickname = elemArr[0] as GtvString
                    val blockchainRid = elemArr[1] as GtvByteArray
                    depList.add(
                            BlockchainRelatedInfo(blockchainRid.bytearray, nickname.string, null)
                    )

                }
                depList.toList()
            } catch (e: Exception) {
                throw BadDataMistake(BadDataType.BAD_CONFIGURATION,
                        "Dependencies must be array of array and have two parts, one string (description) and one bytea (blokchain RID)", e)
            }
        } else {
            // It is allowed to have no dependencies
            listOf<BlockchainRelatedInfo>()
        }
    }

    companion object {

        @Deprecated("Deprecated in v2.4.4. Will be deleted in v3.0")
        fun readFromCommonsConfiguration(config: Configuration, chainId: Long, blockchainRID: ByteArray, nodeID: Int):
                BaseBlockchainConfigurationData {

            val gtxConfig = convertConfigToGtv(config.subset("blockchain.$chainId")) as GtvDictionary
            val cryptoSystem = SECP256K1CryptoSystem()
            val privKey = gtxConfig["blocksigningprivkey"]!!.asByteArray()
            val pubKey = secp256k1_derivePubKey(privKey)
            val sigMaker = cryptoSystem.buildSigMaker(pubKey, privKey) // TODO: maybe take it from somewhere?

            return BaseBlockchainConfigurationData(
                    gtxConfig,
                    BaseBlockchainContext(blockchainRID, nodeID, chainId, pubKey),
                    sigMaker)
        }

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

        @Deprecated("Deprecated in v2.4.4. Will be deleted in v3.0")
        private fun convertConfigToGtv(config: Configuration): Gtv {

            fun blockStrategy(config: Configuration): Gtv {
                return gtv(
                        "name" to gtv(config.getString("blockstrategy"))
                )
            }

            val properties = mutableListOf(
                    "blockstrategy" to blockStrategy(config),
                    "configurationfactory" to gtv(config.getString("configurationfactory")),
                    "signers" to gtv(config.getStringArray("signers").map { gtv(it.hexStringToByteArray()) }),
                    "blocksigningprivkey" to gtv(config.getString("blocksigningprivkey").hexStringToByteArray())
            )

            if (config.containsKey("gtx.modules")) {
                properties.add(Pair("gtx", convertGTXConfigToGtv(config)))
            }

            return gtv(*properties.toTypedArray())
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
