package net.postchain.base.data

/**
 * Implementation for SAP HANA
 */
object SAPHanaSQLCommands : SQLCommands {

    override val createTableBlocks: String = "CREATE TABLE blocks" +
            " (block_iid BIGSERIAL PRIMARY KEY," +
            "  block_height BIGINT NOT NULL, " +
            "  block_rid BYTEA," +
            "  chain_id BIGINT NOT NULL," +
            "  block_header_data BYTEA," +
            "  block_witness BYTEA," +
            "  timestamp BIGINT," +
            "  UNIQUE (chain_id, block_rid)," +
            "  UNIQUE (chain_id, block_height))"

    override val createTableBlockChains: String = "CREATE TABLE blockchains " +
            " (chain_id BIGINT PRIMARY KEY, blockchain_rid BYTEA NOT NULL)"

    override val createTableTransactions: String = "CREATE TABLE transactions (" +
            "    tx_iid BIGSERIAL PRIMARY KEY, " +
            "    chain_id bigint NOT NULL," +
            "    tx_rid bytea NOT NULL," +
            "    tx_data bytea NOT NULL," +
            "    tx_hash bytea NOT NULL," +
            "    block_iid bigint NOT NULL REFERENCES blocks(block_iid)," +
            "    UNIQUE (chain_id, tx_rid))"

    override val createTableConfiguration = "CREATE TABLE configurations (" +
            " chain_id bigint NOT NULL" +
            ", height BIGINT NOT NULL" +
            ", configuration_data bytea NOT NULL" +
            ", PRIMARY KEY (chain_id, height)" +
            ")"

    override val createTableMeta : String = "CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)"

    override val insertBlocks: String = "INSERT INTO blocks (chain_id, block_height) VALUES (?, ?) RETURNING block_iid"

    override val insertTransactions : String = "INSERT INTO transactions (chain_id, tx_rid, tx_data, tx_hash, block_iid) " +
            "VALUES (?, ?, ?, ?, ?) RETURNING tx_iid"

    override val insertConfiguration : String = "INSERT INTO configurations (chain_id, height, configuration_data) VALUES (?, ?, ?) " +
            "ON CONFLICT (chain_id, height) DO UPDATE SET configuration_data = ?"

}