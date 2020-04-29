package net.postchain.devtools.utils.configuration.pre


import mu.KLogging
import net.postchain.base.BaseBlockchainConfigurationData.Companion.KEY_BLOCKSTRATEGY
import net.postchain.base.BaseBlockchainConfigurationData.Companion.KEY_BLOCKSTRATEGY_NAME
import net.postchain.base.BaseBlockchainConfigurationData.Companion.KEY_CONFIGURATIONFACTORY
import net.postchain.base.BaseBlockchainConfigurationData.Companion.KEY_SIGNERS
import net.postchain.base.BaseBlockchainConfigurationData.Companion.KEY_GTX
import net.postchain.base.BaseBlockchainConfigurationData.Companion.KEY_GTX_MODULES
import net.postchain.base.BaseBlockchainConfigurationData.Companion.KEY_DEPENDENCIES
import net.postchain.base.BaseDependencyFactory
import net.postchain.base.BlockchainRelatedInfo
import net.postchain.common.hexStringToByteArray
import net.postchain.devtools.KeyPairHelper
import net.postchain.devtools.utils.configuration.NodeSeqNumber
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import java.lang.IllegalArgumentException


/**
 * This setup holds the most important facts about a blockchain we will use during the test.
 * This class is used for test where we don't have a configuration file, and need to generate the GTV configuration.
 * (The goal is to generate a [BlockchainSetup] instance from this instance.)
 * Since we don't have the GTV configuration, we also don't know what the Blockchain RID will be (since it's calculated from config).
 *
 * (The "setup" classes are data holders/builders for test configuration used to generate the "real" classes at a later stage)
 *
 * @property chainId is the ID of the chain
 * @property signerKeys is a map of the priv and pub keys of each node
 * @property chainDependencies is a set of the other chain IDs this chain depends on
 */
data class BlockchainPreSetup(
        val chainId: Int,
        val signersKeys: Map<NodeSeqNumber, Pair<String, String>>, // First value is PUB second is PRIV
        val chainDependencies: Set<Int> = setOf() // default is none
) {


    companion object : KLogging() {

        const val DEFAULT_BLOCKSTRATEGY = "net.postchain.devtools.OnDemandBlockBuildingStrategy"
        const val DEFAULT_CONFIGURATIONFACTORY = "net.postchain.gtx.GTXBlockchainConfigurationFactory" // We use real GTX as default
                                // another option: net.postchain.devtools.testinfra.TestBlockchainConfigurationFactory
        const val DEFAULT_MODULE_TEST = "net.postchain.configurations.GTXTestModule"
        const val DEFAULT_MODULE_STD_GTX_OPS = "net.postchain.gtx.StandardOpsGTXModule"

        /**
         * Will:
         * 1. figure out the PUB and PRIV keys for each node
         * 2. use no dependencies between chains
         */
        fun simpleBuild(chainId: Int, signers: List<NodeSeqNumber>): BlockchainPreSetup {
            return BlockchainPreSetup(chainId, pubPrivKeyGenerator(signers))
        }


        /**
         * Same as above but with BC dependencies
         */
        fun buildWithDependencies(
                chainId: Int,
                signers: List<NodeSeqNumber>,
                dependencyChainIids: Set<Int>
        ): BlockchainPreSetup {
            return BlockchainPreSetup(chainId, pubPrivKeyGenerator(signers), dependencyChainIids)
        }

        private fun pubPrivKeyGenerator(signers: List<NodeSeqNumber>): Map<NodeSeqNumber, Pair<String, String>> {
            val signersKeys = mutableMapOf<NodeSeqNumber, Pair<String, String>>()
            for (nodeSeq in signers) {
                val pubKey = KeyPairHelper.pubKeyHex(nodeSeq.nodeNumber)
                val privKey = KeyPairHelper.privKeyHex(nodeSeq.nodeNumber)
                signersKeys[nodeSeq] = Pair(pubKey, privKey)
            }
            return signersKeys.toMap()
        }

    }

    /**
     * @param depsToBeMatched is a list of dependencies we can use
     * @return true if all the given dependencies are enough to match the deps of this blockchain
     */
    fun allDependenciesMatched(depsToBeMatched: Set<Int>): Boolean {
        for (dep: Int in chainDependencies) {
            if (!isDependencyInList(dep, depsToBeMatched)) {
                return false
            }
        }

        return true
    }

    private fun isDependencyInList(depToFind: Int, depsToBeMatched: Set<Int>): Boolean {
        return if (depsToBeMatched.contains(depToFind)) {
            true
        } else {
            logger.warn("The chain $chainId has a dependency $depToFind that cannot be matched")
            false
        }
    }


    /**
     * The point of this class is to be able to generate a GTV configuration from the data in this class
     * (so we can build a [BlockchainSetup] from the GTV conf at the next step).
     *
     * @param existingBcMap holds all the BC RID we already calculated
     *                      NOTE!!! If we are calling [toGtvConfig] in the wrong order we might not have the BC RID we need to
     *                              build the dependency list. Therefore the content in this map has to be correct.
     * @return a configuration in Gtv format, filled with default values. (Take this and just override anything you don't like)
     */
    fun toGtvConfig(existingBcMap: Map<Int, BlockchainRelatedInfo>): Gtv {

        val properties = mutableListOf(
                KEY_BLOCKSTRATEGY  to GtvFactory.gtv( KEY_BLOCKSTRATEGY_NAME to GtvFactory.gtv(DEFAULT_BLOCKSTRATEGY)),
                KEY_CONFIGURATIONFACTORY to GtvFactory.gtv(DEFAULT_CONFIGURATIONFACTORY),
                KEY_SIGNERS to GtvFactory.gtv(signersKeys.values.map { GtvFactory.gtv(it.first.hexStringToByteArray()) }),
                KEY_GTX to fixGtxPart()
        )

        if (!chainDependencies.isEmpty()) {
            val depsInfo = mutableListOf<BlockchainRelatedInfo>()
            for (depChainId in chainDependencies) {
                val depInfo = existingBcMap[depChainId]
                if (depInfo != null) {
                    depsInfo.add(depInfo)
                } else {
                    throw IllegalArgumentException("You are generating GTV in the incorrect order. This BC ($chainId) depends on $depChainId but $depChainId has no BC RID yet.")
                }
            }
            properties.add(KEY_DEPENDENCIES to BaseDependencyFactory.buildGtv(depsInfo.toList())!!)
        }

        return GtvFactory.gtv(*properties.toTypedArray())
    }

    /**
     * Will build the GTX part of the configuration, with default modules in it.
     */
    private fun fixGtxPart(): Gtv{
        val modules = listOf(DEFAULT_MODULE_TEST, DEFAULT_MODULE_STD_GTX_OPS)

        val properties: MutableList<Pair<String, Gtv>> = mutableListOf(
                KEY_GTX_MODULES to GtvFactory.gtv(
                        modules.map { GtvFactory.gtv(it) }
                )
        )
        return GtvFactory.gtv(*properties.toTypedArray())

    }

}