package net.postchain.config.node

/**
 * Provides general configuration of node like database settings, REST API settings, etc.
 * Peers settings (IP addresses, public keys, etc.) are also here.
 */
interface NodeConfigurationProvider {
    fun getConfiguration(): NodeConfig
}