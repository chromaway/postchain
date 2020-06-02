// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.cli

import net.postchain.PostchainNode
import net.postchain.StorageBuilder
import net.postchain.cli.AlreadyExistMode
import net.postchain.cli.CliExecution
import net.postchain.config.app.AppConfig
import net.postchain.config.node.NodeConfigurationProviderFactory
import net.postchain.core.BlockQueries
import net.postchain.core.NODE_ID_NA
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Paths

@Ignore
class CliIntegrationTest {

    private fun fullPath(name: String): String {
        return Paths.get(javaClass.getResource("/net/postchain/devtools/cli/${name}").toURI()).toString()
    }

    @Test
    fun testModule() {
        val nodeConfigPath = fullPath("node-config.properties")
        val appConfig = AppConfig.fromPropertiesFile(nodeConfigPath)
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(appConfig)

        // this wipes the data base!
        StorageBuilder.buildStorage(appConfig, NODE_ID_NA, true)

        // add-blockchain goes here
        val chainId: Long = 1;
        val blockChainConfig = fullPath("blockchain_config_4_signers.xml")
        val brid = CliExecution.addBlockchain(nodeConfigPath, chainId, blockChainConfig, AlreadyExistMode.FORCE)

        val node = PostchainNode(nodeConfigProvider)
        node.startBlockchain(chainId)
        val chain = node.processManager.retrieveBlockchain(chainId)
        val queries = chain!!.getEngine().getBlockQueries()

        for (x in 0..1000) {
            Thread.sleep(10)
            if (queries.getBestHeight().get() > 5) {
                break
            }
        }

        println("Stop all blockchain")
        node.shutdown()
    }

    @Test
    fun testModuleWithSAPDatabase() {
        val nodeConfigPath = fullPath("node-config-saphana.properties")
        val appConfig = AppConfig.fromPropertiesFile(nodeConfigPath)
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(appConfig)

        // this wipes the data base!
        StorageBuilder.buildStorage(appConfig, NODE_ID_NA, true)

        // add-blockchain goes here
        val chainId: Long = 1;
        val blockChainConfig = fullPath("blockchain_config_4_signers.xml")
        CliExecution.addBlockchain(nodeConfigPath, chainId, blockChainConfig, AlreadyExistMode.FORCE)

        val node = PostchainNode(nodeConfigProvider)
        node.startBlockchain(chainId)
        val chain = node.processManager.retrieveBlockchain(chainId)
        val queries = chain!!.getEngine().getBlockQueries()

        for (x in 0..1000) {
            Thread.sleep(10)
            if (queries.getBestHeight().get() > 1) {
                break
            }
        }

        println("Stop all blockchain")
        node.shutdown()
    }

    @Test
    fun testAddConfiguration() {
        val nodeConfigPath = fullPath("node-config.properties")
        val appConfig = AppConfig.fromPropertiesFile(nodeConfigPath)
        val nodeConfigProvider = NodeConfigurationProviderFactory.createProvider(appConfig)

        // this wipes the data base!
        StorageBuilder.buildStorage(appConfig, NODE_ID_NA, true)

        // add-blockchain goes here
        val chainId = 1L
        val blockChainConfig = fullPath("blockchain_config.xml")
        val brid = CliExecution.addBlockchain(nodeConfigPath, chainId, blockChainConfig, AlreadyExistMode.FORCE)

        // start blockchain with one signer first
        val node = PostchainNode(nodeConfigProvider)
        node.startBlockchain(chainId)
        val chain = node.processManager.retrieveBlockchain(chainId)
        val queries = chain!!.getEngine().getBlockQueries()

        waitUntilBlock(queries, 1, 100) // wait to build first block
        println(queries.getBestHeight().get())
        Assert.assertTrue(queries.getBestHeight().get() >= 1) // make sure it built at least one block

        // change configuration with 4 signer and height is 10
        val secondBlockChainConfig = fullPath("blockchain_config_4_signers.xml")
        CliExecution.addConfiguration(nodeConfigPath, secondBlockChainConfig, chainId, 10L, AlreadyExistMode.FORCE)

        Assert.assertTrue("Internal problem with the test", queries.getBestHeight().get() < 10)
        waitUntilBlock(queries, 10, 500) // wait until node builds 10 blocks
        println(queries.getBestHeight().get())

        Assert.assertTrue(queries.getBestHeight().get() == 10L)
        waitUntilBlock(queries, 11, 200) // this should exit after 200 milliseconds
        Assert.assertTrue(queries.getBestHeight().get() == 10L)

        node.shutdown()
    }

    private fun waitUntilBlock(queries: BlockQueries, height: Int, maxWaitTime: Int) {
        var count: Int = 0;
        while (count < maxWaitTime) {
            Thread.sleep(10)
            if (queries.getBestHeight().get() >= height) {
                break;
            }
            count++
        }
    }
}