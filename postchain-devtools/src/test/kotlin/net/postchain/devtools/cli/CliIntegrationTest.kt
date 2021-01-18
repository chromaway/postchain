// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.cli

import net.postchain.StorageBuilder
import net.postchain.cli.AlreadyExistMode
import net.postchain.cli.CliExecution
import net.postchain.common.toHex
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.core.NODE_ID_NA
import org.junit.Before
import org.junit.Test
import java.nio.file.Paths
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

/* schema name: test0 */
class CliIntegrationTest {

    val nodeConfigPath = fullPath("node-config.properties")
    val appConfig = AppConfig.fromPropertiesFile(nodeConfigPath)
    val chainId = 1L
    val extectedBlockchainRID = "3475C1EEC5836D9B38218F78C30D302DBC7CAAAFFAF0CC83AE054B7A208F71D4"
    val secondBlockChainConfig = fullPath("blockchain_config_4_signers.xml")
    val heightSecondConfig = 10L

    private fun fullPath(name: String): String {
        return Paths.get(javaClass.getResource("/net/postchain/devtools/cli/${name}").toURI()).toString()
    }

    @Before
    fun setup() {
        // this wipes the data base.
        StorageBuilder.buildStorage(appConfig, NODE_ID_NA, true)
        // add-blockchain goes here
        val blockChainConfig = fullPath("blockchain_config.xml")
        CliExecution.addBlockchain(nodeConfigPath, chainId, blockChainConfig, AlreadyExistMode.FORCE)


    }

    @Test
    fun testAddConfigurationMissingPeerinfo() {
        try {
            // change configuration with 4 signer at height 10
            CliExecution.addConfiguration(nodeConfigPath, secondBlockChainConfig, chainId, heightSecondConfig, AlreadyExistMode.FORCE,
                    false)
            fail()
        } catch (e: net.postchain.cli.CliError.Companion.CliException) {
            // asser config added
            val configData = CliExecution.getConfiguration(nodeConfigPath, chainId, heightSecondConfig)
            assertNull(configData)
        }
    }

    @Test
    fun testAddConfigurationAllowUnknownSigners() {
        // change configuration with 4 signer at height 10
        CliExecution.addConfiguration(nodeConfigPath, secondBlockChainConfig, chainId, heightSecondConfig, AlreadyExistMode.FORCE,
                true)
        // assert bc added
        CliExecution.checkBlockchain(nodeConfigPath, chainId, extectedBlockchainRID)
        // asser config added
        val configData = CliExecution.getConfiguration(nodeConfigPath, chainId, heightSecondConfig)
        assertNotNull(configData)
    }

    @Test
    fun testAddConfigurationPeersAdded() {
        // add peerinfos for the new signers.
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(appConfig)
        val peerinfos = nodeConfigProvider.getConfiguration().peerInfoMap
        for ((_, value) in peerinfos) {
            CliExecution.peerinfoAdd(nodeConfigPath, value.host, value.port, value.pubKey.toHex(),
                    AlreadyExistMode.FORCE)
        }
        // change configuration with 4 signer and height is 10
        val secondBlockChainConfig = fullPath("blockchain_config_4_signers.xml")
        CliExecution.addConfiguration(nodeConfigPath, secondBlockChainConfig, chainId, heightSecondConfig, AlreadyExistMode.FORCE,
                false)
        // assert bc added
        CliExecution.checkBlockchain(nodeConfigPath, chainId, extectedBlockchainRID)
        // asser config added
        val configData = CliExecution.getConfiguration(nodeConfigPath, chainId, heightSecondConfig)
        assertNotNull(configData)
    }

}