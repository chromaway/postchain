package net.postchain.integrationtest.managedmode

import mu.KLogging
import net.postchain.core.EContext
import net.postchain.gtv.*
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.gtx.SimpleGTXModule
import net.postchain.integrationtest.managedmode.TestModulesHelper.argCurrentHeight
import net.postchain.integrationtest.managedmode.TestModulesHelper.argHeight
import net.postchain.integrationtest.managedmode.TestModulesHelper.gtvBlockchain0Rid
import net.postchain.integrationtest.managedmode.TestModulesHelper.peerInfoToGtv
import net.postchain.integrationtest.managedmode.TestPeerInfos.Companion.peerInfo0
import net.postchain.integrationtest.managedmode.TestPeerInfos.Companion.peerInfo1

open class ManagedTestModuleTwoPeersConnect(node: Nodes) : SimpleGTXModule<ManagedTestModuleTwoPeersConnect.Companion.Nodes>(
        node,
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

        enum class Nodes {
            Node0, Node1
        }

        private val stage0 = -1 until 15
        private val stage1 = 15 until 30

        fun queryGetPeerInfos(node: Nodes, eContext: EContext, args: Gtv): Gtv {
            logger.error { "Query: nm_get_peer_infos" }

            if (argCurrentHeight(args) in stage0)
                logger.error { "in range 0" }
            if (argCurrentHeight(args) in stage1)
                logger.error { "in range 1" }

            val peerInfos = when (argCurrentHeight(args)) {

                in stage0 -> {
                    when (node) {
                        Nodes.Node0 -> arrayOf(peerInfo0)
                        Nodes.Node1 -> arrayOf(peerInfo1)
                    }
                }

                in stage1 -> {
                    arrayOf(peerInfo0, peerInfo1)
                }

                else -> emptyArray()
            }

            return GtvArray(peerInfos
                    .map(::peerInfoToGtv)
                    .toTypedArray()
            )
        }

        fun queryGetPeerListVersion(node: Nodes, eContext: EContext, args: Gtv): Gtv {
            logger.error { "Query: nm_get_peer_list_version" }

            if (argCurrentHeight(args) in stage0)
                logger.error { "in range 0" }
            if (argCurrentHeight(args) in stage1)
                logger.error { "in range 1" }

            val version = when (argCurrentHeight(args)) {
                in stage0 -> 1
                in stage1 -> 2
                else -> 2
            }

            return GtvInteger(version.toLong())
        }

        fun queryComputeBlockchainList(node: Nodes, eContext: EContext, args: Gtv): Gtv {
            logger.error { "Query: nm_compute_blockchain_list" }
            return GtvArray(arrayOf(gtvBlockchain0Rid()))
        }

        fun queryGetConfiguration(node: Nodes, eContext: EContext, args: Gtv): Gtv {
            logger.error { "Query: nm_get_blockchain_configuration" }

            val blockchainConfigFilename = when (node) {
                Nodes.Node0 -> "/net/postchain/devtools/managedmode/blockchain_config_two_peers_connect_0_height_15.xml"
                Nodes.Node1 -> "/net/postchain/devtools/managedmode/blockchain_config_two_peers_connect_1_height_15.xml"
            }

            val gtvConfig = GtvMLParser.parseGtvML(
                    javaClass.getResource(blockchainConfigFilename).readText())

            val encodedGtvConfig = GtvEncoder.encodeGtv(gtvConfig)

            return GtvFactory.gtv(encodedGtvConfig)
        }

        fun queryFindNextConfigurationHeight(node: Nodes, eContext: EContext, args: Gtv): Gtv {
            logger.error { "Query: nm_find_next_configuration_height" }

            return if (argHeight(args) < 16)
                GtvInteger(16)
            else GtvNull
        }
    }

}

class ManagedTestModuleTwoPeersConnect0() : ManagedTestModuleTwoPeersConnect(Companion.Nodes.Node0)

class ManagedTestModuleTwoPeersConnect1() : ManagedTestModuleTwoPeersConnect(Companion.Nodes.Node1)

