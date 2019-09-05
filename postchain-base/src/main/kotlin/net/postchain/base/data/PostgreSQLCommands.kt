package net.postchain.base.data

/**
 * Implementation for PostgresSQL
 */

object PostgreSQLCommands : SQLCommands {

    override val createTableBlocks: String = "CREATE TABLE blocks" +
            " (block_iid BIGSERIAL PRIMARY KEY," +
            "  block_height BIGINT NOT NULL, " +
            "  block_rid BYTEA," +
            "  chain_iid BIGINT NOT NULL," +
            "  block_header_data BYTEA," +
            "  block_witness BYTEA," +
            "  timestamp BIGINT," +
            "  UNIQUE (chain_iid, block_rid)," +
            "  UNIQUE (chain_iid, block_height))"

    override val createTableBlockChains: String = "CREATE TABLE blockchains " +
            " (chain_iid BIGINT PRIMARY KEY, blockchain_rid BYTEA NOT NULL)"

    override val createTableTransactions: String = "CREATE TABLE transactions (" +
            "    tx_iid BIGSERIAL PRIMARY KEY, " +
            "    chain_iid bigint NOT NULL," +
            "    tx_rid bytea NOT NULL," +
            "    tx_data bytea NOT NULL," +
            "    tx_hash bytea NOT NULL," +
            "    block_iid bigint NOT NULL REFERENCES blocks(block_iid)," +
            "    UNIQUE (chain_iid, tx_rid))"

    override val createTableConfiguration = "CREATE TABLE configurations (" +
            " chain_iid bigint NOT NULL" +
            ", height BIGINT NOT NULL" +
            ", configuration_data bytea NOT NULL" +
            ", PRIMARY KEY (chain_iid, height)" +
            ")"

    override val createTablePeerInfos = "CREATE TABLE ${SQLDatabaseAccess.TABLE_PEERINFOS} (" +
            " ${SQLDatabaseAccess.TABLE_PEERINFOS_FIELD_HOST} text NOT NULL" +
            ", ${SQLDatabaseAccess.TABLE_PEERINFOS_FIELD_PORT} integer NOT NULL" +
            ", ${SQLDatabaseAccess.TABLE_PEERINFOS_FIELD_PUBKEY} text NOT NULL" +
            ", ${SQLDatabaseAccess.TABLE_PEERINFOS_FIELD_CREATED_AT} timestamp NOT NULL" +
            ", ${SQLDatabaseAccess.TABLE_PEERINFOS_FIELD_UPDATED_AT} timestamp NOT NULL" +
            ")"

    override val createTableMeta: String = "CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)"

    override val insertBlocks: String = "INSERT INTO blocks (chain_iid, block_height) VALUES (?, ?)"

    override val insertTransactions: String = "INSERT INTO transactions (chain_iid, tx_rid, tx_data, tx_hash, block_iid) " +
            "VALUES (?, ?, ?, ?, ?)"

    override val insertConfiguration: String = "INSERT INTO configurations (chain_iid, height, configuration_data) VALUES (?, ?, ?) " +
            "ON CONFLICT (chain_iid, height) DO UPDATE SET configuration_data = ?"

    override val createTableGtxModuleVersion: String = "CREATE TABLE gtx_module_version (module_name TEXT PRIMARY KEY, version BIGINT NOT NULL)"

    override fun isSavepointSupported(): Boolean = true

    override fun dropSchemaCascade(schema: String): String {
        return "DROP SCHEMA IF EXISTS ${schema} CASCADE"
    }

    override fun createSchema(schema: String): String {
        return "CREATE SCHEMA IF NOT EXISTS ${schema}"
    }

    override fun setCurrentSchema(schema: String): String {
        return "set search_path to $schema"
    }
}