// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

import net.postchain.base.BlockchainRid
import net.postchain.base.Storage
import net.postchain.gtv.Gtv

/**
 * BlockchainConfiguration is a stateless objects which describes
 * an individual blockchain instance within Postchain system
 */
interface BlockchainConfiguration {
    val chainID: Long
    val blockchainRid: BlockchainRid
    val traits: Set<String>

    fun decodeBlockHeader(rawBlockHeader: ByteArray): BlockHeader
    fun decodeWitness(rawWitness: ByteArray): BlockWitness
    fun getTransactionFactory(): TransactionFactory
    fun makeBlockBuilder(ctx: EContext): BlockBuilder
    fun makeBlockQueries(storage: Storage): BlockQueries
    fun initializeDB(ctx: EContext)
    fun getBlockBuildingStrategy(blockQueries: BlockQueries, txQueue: TransactionQueue): BlockBuildingStrategy
}

interface ConfigurationDataStore {
    fun findConfigurationHeightForBlock(context: EContext, height: Long): Long?
    fun getConfigurationData(context: EContext, height: Long): ByteArray?
    fun addConfigurationData(context: EContext, height: Long, binData: ByteArray)
    fun addConfigurationData(context: EContext, height: Long, gtvData: Gtv, allowUnknownSigners: Boolean = true)
    // setting default value of flag allowUnknownSigners = true to not risk breaking tests without populated peerinfo table.
}

interface BlockchainConfigurationFactory {
    fun makeBlockchainConfiguration(configurationData: Any): BlockchainConfiguration
}
