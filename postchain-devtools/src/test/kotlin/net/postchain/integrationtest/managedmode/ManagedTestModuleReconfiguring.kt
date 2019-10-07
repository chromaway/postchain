package net.postchain.integrationtest.managedmode

import mu.KLogging
import net.postchain.core.EContext
import net.postchain.gtv.*
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.gtx.SimpleGTXModule
import net.postchain.integrationtest.managedmode.TestModulesHelper.gtvBlockchain0Rid
import net.postchain.integrationtest.managedmode.TestModulesHelper.peerInfoToGtv
import net.postchain.integrationtest.managedmode.TestPeerInfos.Companion.peerInfo0

open class ManagedTestModuleReconfiguring(val stage: Int) : SimpleGTXModule<Unit>(
        Unit,
        mapOf(),
        mapOf(
                "nm_get_peer_infos" to ::queryGetPeerInfos,
                "nm_get_peer_list_version" to ::queryGetPeerListVersion,
                "nm_compute_blockchain_list" to ::queryComputeBlockchainList,
                "nm_get_blockchain_configuration" to ::queryGetConfiguration,
                "nm_find_next_configuration_height" to ::queryFindNextConfigurationHeight
        )
) {

    override fun initializeDB(ctx: EContext) {}

    companion object : KLogging() {

        private val stage0 = -1 until 15
        private val stage1 = 15 until 30
        private val stage2 = 30 until 45
        private val stage3 = 45 until 60

        fun queryGetPeerInfos(unit: Unit, eContext: EContext, args: Gtv): Gtv {
            logger.error { "Query: nm_get_peer_infos" }
            return GtvArray(arrayOf(
                    peerInfoToGtv(peerInfo0))
            )
        }

        fun queryGetPeerListVersion(unit: Unit, eContext: EContext, args: Gtv): Gtv {
            logger.error { "Query: nm_get_peer_list_version" }
            return GtvInteger(1L)
        }

        fun queryComputeBlockchainList(unit: Unit, eContext: EContext, args: Gtv): Gtv {
            logger.error { "Query: nm_compute_blockchain_list" }
            return GtvArray(arrayOf(gtvBlockchain0Rid()))
        }

        fun queryGetConfiguration(unit: Unit, eContext: EContext, args: Gtv): Gtv {
            logger.error { "Query: nm_get_blockchain_configuration" }

            logger.warn { "height: ${args["height"]!!.asInteger()}" }

            val blockchainConfigFilename = when (args["height"]!!.asInteger()) {
                15L -> "/net/postchain/devtools/managedmode/blockchain_config_reconfiguring_15.xml"
                30L -> "/net/postchain/devtools/managedmode/blockchain_config_reconfiguring_30.xml"
                45L -> "/net/postchain/devtools/managedmode/blockchain_config_reconfiguring_45.xml"
                else -> "an unreachable branch"
            }

            logger.warn { "blockchainConfigFilename: $blockchainConfigFilename" }

            val gtvConfig = GtvMLParser.parseGtvML(
                    javaClass.getResource(blockchainConfigFilename).readText())
            val encodedGtvConfig = GtvEncoder.encodeGtv(gtvConfig)
            return GtvFactory.gtv(encodedGtvConfig)
        }

        fun queryFindNextConfigurationHeight(unit: Unit, eContext: EContext, args: Gtv): Gtv {
            logger.error { "Query: nm_find_next_configuration_height" }
            return when (args["height"]!!.asInteger()) {
                in stage0 -> GtvInteger(15)
                in stage1 -> GtvInteger(30)
                in stage2 -> GtvInteger(45)
                else -> GtvNull
            }
        }

    }
}

class ManagedTestModuleReconfiguring0 : ManagedTestModuleReconfiguring(0)

class ManagedTestModuleReconfiguring1 : ManagedTestModuleReconfiguring(1)

class ManagedTestModuleReconfiguring2 : ManagedTestModuleReconfiguring(2)

class ManagedTestModuleReconfiguring3 : ManagedTestModuleReconfiguring(3)

