// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.config

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import net.postchain.base.PeerInfo
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.runStorageCommand
import net.postchain.common.hexStringToByteArray
import net.postchain.config.app.AppConfig
import net.postchain.devtools.ConfigFileBasedIntegrationTest
import net.postchain.devtools.PostchainTestNode.Companion.DEFAULT_CHAIN_IID
import net.postchain.devtools.assertChainStarted
import net.postchain.devtools.assertNodeConnectedWith
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore
class ManualNodeConfigProviderTest : ConfigFileBasedIntegrationTest() {

    private val peerInfos = arrayOf(
            PeerInfo(
                    "127.0.0.1", 9870,
                    "03a301697bdfcd704313ba48e51d567543f2a182031efd6915ddc07bbcc4e16070".hexStringToByteArray()),
            PeerInfo(
                    "127.0.0.1", 9871,
                    "031B84C5567B126440995D3ED5AABA0565D71E1834604819FF9C17F5E9D5DD078F".hexStringToByteArray()),
            PeerInfo(
                    "127.0.0.1", 9872,
                    "03B2EF623E7EC933C478135D1763853CBB91FC31BA909AEC1411CA253FDCC1AC94".hexStringToByteArray()),
            PeerInfo(
                    "127.0.0.1", 9873,
                    "0203C6150397F7E4197FF784A8D74357EF20DAF1D09D823FFF8D3FC9150CBAE85D".hexStringToByteArray())
    )

    @Before
    fun setUp() {
        setUpNode(0)
        setUpNode(1)
        setUpNode(2)
        setUpNode(3)
    }

    @After
    override fun tearDown() {
        tearDownNode(0)
        tearDownNode(1)
        tearDownNode(2)
        tearDownNode(3)
        super.tearDown()
    }

    @Test
    fun fourPeersConnectedSuccessfully() {
        val nodesCount = 4
        val blockchainConfig = "/net/postchain/devtools/config/blockchain_config_4.xml"
        val nodeConfigs = arrayOf(
                "classpath:/net/postchain/config/node0.properties",
                "classpath:/net/postchain/config/node1.properties",
                "classpath:/net/postchain/config/node2.properties",
                "classpath:/net/postchain/config/node3.properties"
        )

        // Creating all peers
        nodeConfigs.forEachIndexed { i, nodeConfig ->
            createSingleNode(i, nodesCount, nodeConfig, blockchainConfig, false)
        }

        // Asserting that
        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted {
                    // chain is active for all peers
                    nodes.forEach { it.assertChainStarted() }

                    // network topology is that peer 3 is disconnected from interconnected peers 0, 1, 2
                    nodes[0].assertNodeConnectedWith(DEFAULT_CHAIN_IID, nodes[1], nodes[2], nodes[3])
                    nodes[1].assertNodeConnectedWith(DEFAULT_CHAIN_IID, nodes[0], nodes[2], nodes[3])
                    nodes[2].assertNodeConnectedWith(DEFAULT_CHAIN_IID, nodes[1], nodes[0], nodes[3])
                    nodes[3].assertNodeConnectedWith(DEFAULT_CHAIN_IID, nodes[1], nodes[2], nodes[0])
                }
    }

    private fun buildAppConfig(nodeIndex: Int): AppConfig {
        return mock {
            on { databaseDriverclass } doReturn "org.postgresql.Driver"
            on { databaseUrl } doReturn "jdbc:postgresql://localhost:5432/postchain"
            on { databaseSchema } doReturn "it_manual_node_config_test_node$nodeIndex"
            on { databaseUsername } doReturn "postchain"
            on { databasePassword } doReturn "postchain"
        }
    }

    private fun setUpNode(nodeIndex: Int) {
        val appConfig = buildAppConfig(nodeIndex)
        runStorageCommand(appConfig) { ctx ->
            peerInfos.forEach { peerInfo ->
                DatabaseAccess.of(ctx).addPeerInfo(ctx, peerInfo)
            }
        }
    }

    private fun tearDownNode(nodeIndex: Int) {
        val appConfig = buildAppConfig(nodeIndex)
        runStorageCommand(appConfig) { ctx ->
            DatabaseAccess.of(ctx).dropSchemaCascade(
                    ctx.conn, appConfig.databaseSchema)
        }
    }

}