package net.postchain.devtools.utils.configuration

import net.postchain.base.BaseDependencyFactory
import net.postchain.common.toHex
import net.postchain.devtools.KeyPairHelper
import net.postchain.gtv.Gtv

object BlockchainSetupFactory {
    /**
     * Builds the [BlockchainSetup] from the config file directly. This is meant to be used only for testing,
     * so we are allowed to make shortcuts.
     *
     * When we create the [BlockchainSetup] we are interested in these values from the config:
     *
     * a) "signers"
     * b) "dependencies"
     *
     * @param chainId usually the chainId is derived from the file path, so we should know it by now.
     * @param bcGtv is the configuration
     * @param fileName is the BC config file name
     */
    fun buildFromGtv(chainId: Int, bcGtv: Gtv, fileName: String, existingBcSetups: List<BlockchainSetup>): BlockchainSetup {

        require(existingBcSetups.filter { it.chainId == chainId }.isEmpty()) {
            "Why are we building a BC Setup with the same chain Id $chainId as we already have in the existing list?"}

        // 1. Get the signers
        val signers = mutableListOf<NodeSeqNumber>()
        val signersArr = bcGtv["signers"]!!
        for (pubkey in signersArr.asArray()) {
            val byteArray = pubkey.asByteArray()
            val nodeId = KeyPairHelper.pubKeyFromByteArray(byteArray.toHex())!! // TODO [olle] this is not safe, sorry. Must have created these in the cache before this
            signers.add(NodeSeqNumber(nodeId))
        }

        // 2 Get dependencies
        val chainRidDependencies = mutableSetOf<String>()
        val dep = bcGtv["dependencies"]
        if (dep != null) {
            val bcRelatedInfos = BaseDependencyFactory.build(dep!!)
            for (bcRelatedInfo in bcRelatedInfos) {
              chainRidDependencies.add(bcRelatedInfo.blockchainRid.toHex())
            }
        }

        return BlockchainSetup.buildWithDependencies(chainId, fileName, bcGtv, signers, chainRidDependencies.toSet())
    }
}