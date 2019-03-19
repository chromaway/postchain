package net.postchain.base.data

interface SQLCommands {
    val createTableBlocks: String
    val createTableBlockChains: String
    val createTableTransactions: String
    val createTableConfiguration : String
    val createTableMeta : String
    val insertBlocks: String
    val checkMetaExists : String
    val insertTransactions : String
    val insertConfiguration : String

}