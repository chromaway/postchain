package net.postchain.devtools.cli

import net.postchain.PostchainNode
import net.postchain.StorageBuilder
import net.postchain.cli.AlreadyExistMode
import net.postchain.cli.CliExecution
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.core.BlockQueries
import net.postchain.core.NODE_ID_NA
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class CliIntegrationTest {

    private fun fullPath(name: String): String {
        return Paths.get(javaClass.getResource("/net/postchain/cli/${name}").toURI()).toString()
    }

    @Test
    fun testModule() {
        val nodeConfigPath = fullPath("node-config.properties")
        val nodeConfig = CommonsConfigurationFactory.readFromFile(nodeConfigPath)

        // this wipes the data base!
        StorageBuilder.buildStorage(nodeConfig, NODE_ID_NA, true)

        // add-blockchain goes here
        val chainId : Long = 1;
        val brid = File(fullPath("brid.txt")).readText()
        val blockChainConfig = fullPath("blockchain_config.xml")
        CliExecution().addBlockchain(nodeConfigPath, chainId, brid, blockChainConfig, AlreadyExistMode.FORCE)

        val node = PostchainNode(nodeConfig)
        node.startBlockchain(chainId)
        val chain = node.processManager.retrieveBlockchain(chainId)
        val queries = chain!!.getEngine().getBlockQueries()

        for (x in 0..1000) {
            Thread.sleep(10)
            if (queries.getBestHeight().get() > 5)  {
                break
            };
        }

        println("Stop all blockchain")
        node.stopAllBlockchain()
    }

    @Test
    fun testAddConfigurationSingleNodeOneSigner() {
        val nodeConfigPath = fullPath("node-config1.properties")
        val nodeConfig = CommonsConfigurationFactory.readFromFile(nodeConfigPath)

        // this wipes the data base!
        StorageBuilder.buildStorage(nodeConfig, NODE_ID_NA, true)

        // add-blockchain goes here
        val chainId : Long = 1;
        val blockChainConfig = fullPath("blockchain_config.xml")
        val height = 1L

        CliExecution().addConfiguration(nodeConfigPath, blockChainConfig, chainId,  height , AlreadyExistMode.FORCE)

        val node = PostchainNode(nodeConfig)
        node.startBlockchain(chainId)
        val chain = node.processManager.retrieveBlockchain(chainId)
        val queries = chain!!.getEngine().getBlockQueries()
        waitUntilBlock(queries, 1, 100)
        Assert.assertTrue(queries.getBestHeight().get() >= 1) // make sure it built at least one block

        node.stopAllBlockchain()
    }


    fun waitUntilBlock(queries: BlockQueries, height: Int, maxWaitTime: Int) {
        var sleepInMilliseconds : Int = 10;
        while(sleepInMilliseconds < maxWaitTime) {
            Thread.sleep(10)
            if (queries.getBestHeight().get() > height) {
                break;
            }
            sleepInMilliseconds += 10;
        }
    }
}