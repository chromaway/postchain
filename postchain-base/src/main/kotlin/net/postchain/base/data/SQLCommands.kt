// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

interface SQLCommands {
    val createTableBlocks: String
    val createTableBlockChains: String
    val createTableTransactions: String
    val createTableConfiguration: String
    val createTablePeerInfos: String
    val createTableMeta: String
    val insertBlocks: String
    val insertTransactions: String
    val insertConfiguration: String
    val createTableGtxModuleVersion: String

    fun isSavepointSupported(): Boolean
    fun dropSchemaCascade(schema: String): String
    fun createSchema(schema: String): String
    fun setCurrentSchema(schema: String): String
}