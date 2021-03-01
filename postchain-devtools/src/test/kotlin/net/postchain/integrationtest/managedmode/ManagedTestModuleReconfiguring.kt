// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.managedmode

import net.postchain.common.hexStringToByteArray
import net.postchain.core.EContext
import net.postchain.gtv.*
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.gtx.SimpleGTXModule
import net.postchain.integrationtest.managedmode.TestModulesHelper.argHeight
import net.postchain.integrationtest.managedmode.TestModulesHelper.peerInfoToGtv
import net.postchain.integrationtest.managedmode.TestPeerInfos.Companion.peerInfo0
import net.postchain.util.TestKLogging

open class ManagedTestModuleReconfiguring(val stage: Int) : SimpleGTXModule<Unit>(
        Unit,
        mapOf(),
        mapOf(
                "nm_get_peer_infos" to ::queryGetPeerInfos,
                "nm_get_peer_list_version" to ::queryGetPeerListVersion,
                "nm_compute_blockchain_list" to ::queryComputeBlockchainList,
                "nm_get_blockchain_configuration" to ::queryGetConfiguration,
                "nm_find_next_configuration_height" to ::queryFindNextConfigurationHeight,
                "nm_get_blockchain_last_height_map" to ::dummyHandlerArray,
                "nm_get_blockchain_replica_node_map" to ::dummyHandlerArray,
                "nm_get_node_replica_map" to ::dummyHandlerArray
        )
) {

    override fun initializeDB(ctx: EContext) {}

    companion object : TestKLogging(LogLevel.DEBUG) {

        private val BLOCKCHAIN_RIDS = mapOf(
                0L to "196F099F825BCE5D426A729F42533529C8AC0255AE26001C34E31B6F25DCC2DF"
        )

        private val stage0 = -1 until 15
        private val stage1 = 15 until 30
        private val stage2 = 30 until 45
        private val stage3 = 45 until 60

        fun queryGetPeerInfos(unit: Unit, eContext: EContext, args: Gtv): Gtv {
            logger.log { "Query: nm_get_peer_infos" }
            return GtvArray(arrayOf(
                    peerInfoToGtv(peerInfo0))
            )
        }

        fun queryGetPeerListVersion(unit: Unit, eContext: EContext, args: Gtv): Gtv {
            logger.log { "Query: nm_get_peer_list_version" }
            return GtvInteger(1L)
        }

        fun queryComputeBlockchainList(unit: Unit, eContext: EContext, args: Gtv): Gtv {
            logger.log { "Query: nm_compute_blockchain_list" }
            return GtvArray(arrayOf(gtvBlockchainRid(0L)))
        }

        fun queryGetConfiguration(unit: Unit, eContext: EContext, args: Gtv): Gtv {
            logger.log {
                "Query: nm_get_blockchain_configuration: " +
                        "height: ${argHeight(args)}, " +
                        "blockchainRid: ${TestModulesHelper.argBlockchainRid(args)}"
            }

            val blockchainConfigFilename = when (argHeight(args)) {
                15L -> "/net/postchain/devtools/managedmode/singlepeer_loads_config_and_reconfigures/blockchain_config_reconfiguring_15.xml"
                30L -> "/net/postchain/devtools/managedmode/singlepeer_loads_config_and_reconfigures/blockchain_config_reconfiguring_30.xml"
                45L -> "/net/postchain/devtools/managedmode/singlepeer_loads_config_and_reconfigures/blockchain_config_reconfiguring_45.xml"
                else -> "an unreachable branch"
            }

            logger.log { "blockchainConfigFilename: $blockchainConfigFilename" }

            return if (blockchainConfigFilename == "an unreachable branch") {
                GtvNull
            } else {
                val gtvConfig = GtvMLParser.parseGtvML(
                        javaClass.getResource(blockchainConfigFilename).readText())
                val encodedGtvConfig = GtvEncoder.encodeGtv(gtvConfig)
                GtvFactory.gtv(encodedGtvConfig)
            }
        }

        fun queryFindNextConfigurationHeight(unit: Unit, eContext: EContext, args: Gtv): Gtv {
            logger.log { "Query: nm_find_next_configuration_height" }
            return when (argHeight(args)) {
                in stage0 -> GtvInteger(15)
                in stage1 -> GtvInteger(30)
                in stage2 -> GtvInteger(45)
                else -> GtvNull
            }
        }

        private fun gtvBlockchainRid(chainId: Long): Gtv {
            return GtvFactory.gtv(
                    BLOCKCHAIN_RIDS[chainId]?.hexStringToByteArray() ?: byteArrayOf())
        }
    }
}

class ManagedTestModuleReconfiguring0 : ManagedTestModuleReconfiguring(0)

class ManagedTestModuleReconfiguring1 : ManagedTestModuleReconfiguring(1)

class ManagedTestModuleReconfiguring2 : ManagedTestModuleReconfiguring(2)

class ManagedTestModuleReconfiguring3 : ManagedTestModuleReconfiguring(3)

