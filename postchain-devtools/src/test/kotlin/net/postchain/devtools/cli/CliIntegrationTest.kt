package net.postchain.devtools.cli

import net.postchain.PostchainNode
import net.postchain.StorageBuilder
import net.postchain.cli.AlreadyExistMode
import net.postchain.cli.CliExecution
import net.postchain.config.CommonsConfigurationFactory
import net.postchain.core.NODE_ID_NA
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
}