package net.postchain.devtools

import net.postchain.config.node.NodeConfigurationProvider
import net.postchain.devtools.utils.configuration.NodeSetup
import net.postchain.devtools.utils.configuration.SystemSetup
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration

/**
 * "Legacy" means there is no "managed mode" with chain zero etc.
 */
object TestLegacyNodeConfigProducer {

    /**
     * Builds a [NodeConfigurationProvider] of type "legacy" and returns it.
     *
     * Here we don't care about the node configuration file (nodeX.properties) at all (most test won't have one).
     *
     * @param testName is the name of the test
     * @param nodeSetup is the node we are working with
     * @param systemSetup is the entire system's config
     * @param startConfig is the config we will use as starting point (but can be overridden by automatic conf), usually nothing

     * @return the [NodeConfigurationProvider] we created
     *
     */
    fun createNodeConfig(testName: String,
                         nodeSetup: NodeSetup,
                         systemSetup: SystemSetup,
                         startConfig: PropertiesConfiguration? = null
    ): Configuration {

        val baseConfig = startConfig ?: PropertiesConfiguration()

        // DB
        setDbConfig(testName, nodeSetup, baseConfig)

        // peers
        setPeerConfig(nodeSetup, systemSetup, baseConfig)

        setSyncTuningParams(systemSetup, baseConfig)

        setConfProvider(systemSetup.nodeConfProvider, baseConfig)
        setConfInfrastructure(systemSetup.confInfrastructure, baseConfig)
        setApiPort(nodeSetup, baseConfig, systemSetup.needRestApi)
        setKeys(nodeSetup, baseConfig)

        return baseConfig
    }

    fun setSyncTuningParams(systemSetup: SystemSetup, baseConfig: PropertiesConfiguration) {
        baseConfig.setProperty("fastsync.exit_delay", if (systemSetup.nodeMap.size == 1) 0 else 1000)
    }

    fun setConfProvider(str: String, baseConfig: PropertiesConfiguration) {
        baseConfig.setProperty("configuration.provider.node", str)
    }

    fun setConfInfrastructure(str: String, baseConfig: PropertiesConfiguration) {
        baseConfig.setProperty("infrastructure", str)
    }

    /**
     * Update the [PropertiesConfiguration] with node info provided by the [NodeSetup]
     */
    fun setDbConfig(
            testName: String, // For example this could be "multiple_chains_node"
            nodeConf: NodeSetup,
            baseConfig: PropertiesConfiguration
    ) {
        // These are the same for all tests
        baseConfig.setProperty("database.driverclass", "org.postgresql.Driver")
        baseConfig.setProperty("database.url",         "jdbc:postgresql://localhost:5432/postchain")
        baseConfig.setProperty("database.username",    "postchain")
        baseConfig.setProperty("database.password",    "postchain")
        // TODO: Maybe a personalized schema name like this is not needed (this is just legacy from the node.properties files)
        val goodTestName = testName.filter { it.isLetterOrDigit() }.toLowerCase()
        baseConfig.setProperty("database.schema", goodTestName + nodeConf.sequenceNumber.nodeNumber)

        // Legacy way of creating nodes, append nodeIndex to schema name
        val dbSchema = baseConfig.getString("database.schema") + "_" + nodeConf.sequenceNumber.nodeNumber

        // To convert negative indexes of replica nodes to 'replica_' prefixed indexes.
        baseConfig.setProperty("database.schema", dbSchema.replace("-", "replica_"))
    }


    /**
     * Updates the [PropertiesConfiguration] with node info provided by the [NodeSetup]
     *
     * @param nodeSetup contains info about the nodes
     * @param baseConfig is the configuration object we will update
     */
    fun setPeerConfig(
            nodeSetup: NodeSetup,
            systemSetup: SystemSetup,
            baseConfig: PropertiesConfiguration
    ) {

        for (nodeNr in nodeSetup.calculateAllNodeConnections(systemSetup)) {
            val i = nodeNr.nodeNumber
            baseConfig.setProperty("node.$i.id", "node${i}")
            baseConfig.setProperty("node.$i.host", "127.0.0.1")
            baseConfig.setProperty("node.$i.port", nodeSetup.getPortNumber())
            baseConfig.setProperty("node.$i.pubkey", nodeSetup.pubKeyHex)
        }
    }

    /**
     * Sets the API port, so it wont clash with other nodes.
     */
    fun setApiPort(
        nodeSetup: NodeSetup,
        baseConfig: PropertiesConfiguration,
        needRestApi: Boolean
    ){
        if (needRestApi) {
            baseConfig.setProperty("api.port", nodeSetup.getApiPortNumber())
        } else {
            baseConfig.setProperty("api.port", -1) // -1 means "don't start"
        }
    }

    /**
     * Sets the pub and priv keys of the node
     */
    fun setKeys(
            nodeConf: NodeSetup,
            baseConfig: PropertiesConfiguration
    ) {

        baseConfig.setProperty("messaging.privkey", nodeConf.privKeyHex)
        baseConfig.setProperty("messaging.pubkey", nodeConf.pubKeyHex)
    }
}