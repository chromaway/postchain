// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

@Deprecated("POS-128")
interface SQLCommands {
    fun configurationsTable(chainId: Long): String

    val createTableBlocks: String
    val createTableBlockChains: String
    val createTableTransactions: String
    val createTableConfiguration: String
    val createTablePeerInfos: String
    val createTableMeta: String
    val insertBlocks: String
    val insertTransactions: String
    val insertConfiguration: String
    fun insertConfigurationCmd(chainId: Long): String
    val createTableGtxModuleVersion: String

    fun isSavepointSupported(): Boolean
    fun dropSchemaCascade(schema: String): String
    fun createSchema(schema: String): String
    fun setCurrentSchema(schema: String): String

    fun pref(chainId: Long): String = "c$chainId."
}