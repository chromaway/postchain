// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.blockchain

enum class BlockchainConfigProviders {

    /**
     * Blockchain parameters are obtained
     * from database
     */
    Manual,

    /**
     * Blockchain parameters are obtained
     * from system blockchain
     */
    Managed
}