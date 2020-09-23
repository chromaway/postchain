// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.managedmode

import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.runStorageCommand
import net.postchain.devtools.ConfigFileBasedIntegrationTest
import net.postchain.hasSize
import net.postchain.integrationtest.assertChainNotStarted
import net.postchain.integrationtest.assertChainStarted
import net.postchain.integrationtest.getModules
import org.awaitility.Awaitility
import org.awaitility.Duration
import org.junit.Ignore
import org.junit.Test

class ManagedModeTest : ConfigFileBasedIntegrationTest() {

    @Test
    fun singlePeer_loadsBlockchain0Configuration_fromManagedDataSource_and_reconfigures() {
        val nodeConfig0 = "classpath:/net/postchain/managedmode/node0.properties"
        val blockchainConfig0 = "/net/postchain/devtools/managedmode/singlepeer_loads_config_and_reconfigures/blockchain_config_reconfiguring_0.xml"

        // Creating node0
        createSingleNode(0, 1, nodeConfig0, blockchainConfig0) { appConfig, _ ->
            runStorageCommand(appConfig) { ctx ->
                DatabaseAccess.of(ctx).addPeerInfo(ctx, TestPeerInfos.peerInfo0)
            }
        }

        // Asserting chain 0 is started for node0
        nodes[0].assertChainStarted(0L)
        val chain0Rid = nodes[0].getBlockchainRid(0L)

        // TODO: [et]: Change comment: Waiting for height 5 when a new peer will be added to PeerInfos
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    assertk.assert(nodes[0].getModules(0L)).isNotEmpty()
                    assertk.assert(nodes[0].getModules(0L).first())
                            .isInstanceOf(ManagedTestModuleReconfiguring0::class)
                }

        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    assertk.assert(nodes[0].getModules(0L)).isNotEmpty()
                    assertk.assert(nodes[0].getModules(0L).first())
                            .isInstanceOf(ManagedTestModuleReconfiguring1::class)
                }

        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    assertk.assert(nodes[0].getModules(0L)).isNotEmpty()
                    assertk.assert(nodes[0].getModules(0L).first())
                            .isInstanceOf(ManagedTestModuleReconfiguring2::class)
                }

        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    assertk.assert(nodes[0].getModules(0L)).isNotEmpty()
                    assertk.assert(nodes[0].getModules(0L).first())
                            .isInstanceOf(ManagedTestModuleReconfiguring3::class)
                }
    }

    @Test
    @Ignore
    fun singlePeer_launchesChain100AtHeight5_then_launchesChain101AtHeight10_then_stopsChain101AtHeight15() {
        val nodeConfig0 = "classpath:/net/postchain/managedmode/node0.properties"
        val blockchainConfig0 = "/net/postchain/devtools/managedmode/singlepeer_launches_and_stops_chains/blockchain_config_0_height_0.xml"

        // Creating node0
        createSingleNode(0, 1, nodeConfig0, blockchainConfig0) { appConfig, _ ->
            runStorageCommand(appConfig) { ctx ->
                DatabaseAccess.of(ctx).addPeerInfo(ctx, TestPeerInfos.peerInfo0)
            }
        }

        // Asserting chain 0 is started and chain 1 and 2 are not
        nodes[0].assertChainStarted(0L)
        nodes[0].assertChainNotStarted(100L)
        nodes[0].assertChainNotStarted(101L)

        // Asserting chain 0, 100 are started and chain 101 is not
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    nodes[0].assertChainStarted(0L)
                    nodes[0].assertChainStarted(100L)
                    nodes[0].assertChainNotStarted(101L)
                }

        // Asserting chain 0, 100, 101 are started
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    nodes[0].assertChainStarted(0L)
                    nodes[0].assertChainStarted(100L)
                    nodes[0].assertChainStarted(101L)
                }

        // Asserting chain 0, 101 are started and chain 100 is not
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    nodes[0].assertChainStarted(0L)
                    nodes[0].assertChainNotStarted(100L)
                    nodes[0].assertChainStarted(101L)

                    // Asserting stage of blockchain:0 is stage3 (15 < height < 20)
                    assertk.assert(nodes[0].getModules(0L)).isNotEmpty()
                    assertk.assert(nodes[0].getModules(0L).first()).isInstanceOf(
                            ManagedTestModuleSinglePeerLaunchesAndStopsChains3::class)
                }
    }

    @Test
    @Ignore
    fun twoSeparatePeers_receiveEachOtherInPeerInfos_and_connectToEachOther() {
        val nodeConfig0 = "classpath:/net/postchain/managedmode/node0.properties"
        val nodeConfig1 = "classpath:/net/postchain/managedmode/node1.properties"
        val blockchainConfig0 = "/net/postchain/devtools/managedmode/two_peers_connect_to_each_other/blockchain_config_two_peers_connect_0.xml"
        val blockchainConfig1 = "/net/postchain/devtools/managedmode/two_peers_connect_to_each_other/blockchain_config_two_peers_connect_1.xml"

        // Creating node0
        createSingleNode(0, 1, nodeConfig0, blockchainConfig0) { appConfig, _ ->
            runStorageCommand(appConfig) { ctx ->
                DatabaseAccess.of(ctx).addPeerInfo(ctx, TestPeerInfos.peerInfo0)
            }
        }

        // Creating node1
        createSingleNode(0, 1, nodeConfig1, blockchainConfig1) { appConfig, _ ->
            runStorageCommand(appConfig) { ctx ->
                DatabaseAccess.of(ctx).addPeerInfo(ctx, TestPeerInfos.peerInfo1)
            }
        }

        // Asserting chain 0 is started for node0
        nodes[0].assertChainStarted(0L)
        assertk.assert(nodes[0].networkTopology(0L)).hasSize(0)

        // Asserting chain 0 is started for node1
        nodes[1].assertChainStarted(0L)
        assertk.assert(nodes[1].networkTopology(0L)).hasSize(0)

        // Waiting for height 5 when a new peer will be added to PeerInfos
        Awaitility.await().atMost(Duration.ONE_MINUTE)
                .untilAsserted {
                    assertk.assert(nodes[0].networkTopology(0L)).hasSize(1)
                    assertk.assert(nodes[1].networkTopology(0L)).hasSize(1)
                }
    }
}