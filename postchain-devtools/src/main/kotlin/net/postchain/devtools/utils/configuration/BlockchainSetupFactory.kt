package net.postchain.devtools.utils.configuration

import mu.KLogging
import net.postchain.base.BaseBlockchainConfigurationData.Companion.KEY_DEPENDENCIES
import net.postchain.base.BaseBlockchainConfigurationData.Companion.KEY_SIGNERS
import net.postchain.base.BaseDependencyFactory
import net.postchain.base.BlockchainRid
import net.postchain.common.toHex
import net.postchain.devtools.KeyPairHelper
import net.postchain.gtv.Gtv
import net.postchain.gtv.gtvml.GtvMLParser

object BlockchainSetupFactory : KLogging() {


    /**
     * Builds the [BlockchainSetup] from the config file's Gtv directly. This is meant to be used only for testing,
     * so we are allowed to make shortcuts.
     * (If you don't have a config file in this test, use [BlockchainPreSetup] to get Gtv config )
     *
     * @param chainIid usually the chainIid is derived from the file path, so we should know it by now.
     * @param blockchainConfigFilename is the file path
     */
    fun buildFromFile(chainIid: Int, blockchainConfigFilename: String): BlockchainSetup {
        val gtv = GtvMLParser.parseGtvML(
                javaClass.getResource(blockchainConfigFilename).readText())
        val res = buildFromGtv(chainIid, gtv)
        logger.debug("Translated Filename: $blockchainConfigFilename -> Setup with bc chainId: $chainIid, bc Rid: ${res.rid.toShortHex()}")
        return res
    }

    /**
     * Builds the [BlockchainSetup] from the config file's Gtv directly. This is meant to be used only for testing,
     * so we are allowed to make shortcuts.
     * (If you don't have a config file in this test, use [BlockchainPreSetup] to get Gtv config )
     *
     * When we create the [BlockchainSetup] we are interested in these values from the config:
     *
     * a) "signers"
     * b) "dependencies"
     *
     * @param chainIid usually the chainIid is derived from the file path, so we should know it by now.
     * @param bcGtv is the configuration
     */
    fun buildFromGtv(chainIid: Int, bcGtv: Gtv): BlockchainSetup {

        // 1. Get the signers
        val signers = mutableListOf<NodeSeqNumber>()
        val signersArr = bcGtv[KEY_SIGNERS]!!

        for (pubkey in signersArr.asArray()) {
            val byteArray = pubkey.asByteArray()
            val nodeId = try {
                // TODO [olle] this is not safe, sorry. Must have created these in the cache before this or it will explode
                KeyPairHelper.pubKeyFromByteArray(byteArray.toHex())!!
            } catch (npe: KotlinNullPointerException) {
                throw IllegalStateException("We mush cache the node Pub key before we can find it: ${byteArray.toHex()}")
            }
            signers.add(NodeSeqNumber(nodeId))
        }

        // 2 Get dependencies
        val chainRidDependencies = mutableSetOf<BlockchainRid>()
        val dep = bcGtv[KEY_DEPENDENCIES]
        if (dep != null) {
            val bcRelatedInfos = BaseDependencyFactory.build(dep!!)
            for (bcRelatedInfo in bcRelatedInfos) {
                chainRidDependencies.add(bcRelatedInfo.blockchainRid)
            }
        }

        return BlockchainSetup.buildWithDependencies(chainIid, bcGtv, signers, chainRidDependencies.toSet())
    }
}