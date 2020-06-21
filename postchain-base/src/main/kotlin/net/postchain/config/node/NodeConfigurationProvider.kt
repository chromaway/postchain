// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.node

/**
 * Provides general configuration of node like database settings, REST API settings, etc.
 * Peers settings (IP addresses, public keys, etc.) are also here.
 */
interface NodeConfigurationProvider : AutoCloseable {
    fun getConfiguration(): NodeConfig
}