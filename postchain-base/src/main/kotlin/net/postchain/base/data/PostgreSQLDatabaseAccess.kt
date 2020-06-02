// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

import net.postchain.core.BlockEContext
import net.postchain.core.EContext
import net.postchain.core.Transaction
import java.sql.Connection

class PostgreSQLDatabaseAccess : SQLDatabaseAccess() {

    override fun isSavepointSupported(): Boolean = true

    override fun createSchema(connection: Connection, schema: String) {
        val sql = "CREATE SCHEMA IF NOT EXISTS $schema"
        queryRunner.update(connection, sql)
    }

    override fun setCurrentSchema(connection: Connection, schema: String) {
        val sql = "SET search_path TO $schema"
        queryRunner.update(connection, sql)
    }

    override fun dropSchemaCascade(connection: Connection, schema: String) {
        val sql = "DROP SCHEMA IF EXISTS $schema CASCADE"
        queryRunner.update(connection, sql)
    }

    override fun cmdCreateTableBlocks(ctx: EContext): String {
        return "CREATE TABLE ${tableBlocks(ctx)}" +
                " (block_iid BIGSERIAL PRIMARY KEY," +
                "  block_height BIGINT NOT NULL, " +
                "  block_rid BYTEA," +
                "  chain_iid BIGINT NOT NULL," +
                "  block_header_data BYTEA," +
                "  block_witness BYTEA," +
                "  timestamp BIGINT," +
                "  UNIQUE (chain_iid, block_rid)," +
                "  UNIQUE (chain_iid, block_height))"
    }

    override fun cmdCreateTableBlockchains(): String {
        return "CREATE TABLE ${tableBlockchains()} " +
                " (chain_iid BIGINT PRIMARY KEY," +
                " blockchain_rid BYTEA NOT NULL)"
    }

    override fun cmdCreateTableTransactions(ctx: EContext): String {
        return "CREATE TABLE ${tableTransactions(ctx)} (" +
                "    tx_iid BIGSERIAL PRIMARY KEY, " +
                "    chain_iid bigint NOT NULL," +
                "    tx_rid bytea NOT NULL," +
                "    tx_data bytea NOT NULL," +
                "    tx_hash bytea NOT NULL," +
                "    block_iid bigint NOT NULL REFERENCES blocks(block_iid)," +
                "    UNIQUE (chain_iid, tx_rid))"
    }

    override fun cmdCreateTableConfigurations(ctx: EContext): String {
        return "CREATE TABLE ${tableConfigurations(ctx)} (" +
                " chain_iid bigint NOT NULL" +
                ", height BIGINT NOT NULL" +
                ", configuration_data bytea NOT NULL" +
                ", PRIMARY KEY (chain_iid, height)" +
                ")"
    }

    override fun cmdCreateTablePeerInfos(): String {
        return "CREATE TABLE ${tablePeerinfos()} (" +
                " $TABLE_PEERINFOS_FIELD_HOST text NOT NULL" +
                ", $TABLE_PEERINFOS_FIELD_PORT integer NOT NULL" +
                ", $TABLE_PEERINFOS_FIELD_PUBKEY text PRIMARY KEY NOT NULL" +
                ", $TABLE_PEERINFOS_FIELD_TIMESTAMP timestamp NOT NULL" +
                ")"
    }

    override fun cmdCreateTableMeta(): String {
        return "CREATE TABLE ${tableMeta()} (key TEXT PRIMARY KEY, value TEXT)"
    }

    override fun cmdInsertBlocks(ctx: EContext): String {
        return "INSERT INTO ${tableBlocks(ctx)} (chain_iid, block_height) VALUES (?, ?)"
    }

    override fun cmdInsertTransactions(ctx: EContext): String {
        return "INSERT INTO ${tableTransactions(ctx)} (chain_iid, tx_rid, tx_data, tx_hash, block_iid) " +
                "VALUES (?, ?, ?, ?, ?)"
    }

    override fun cmdInsertConfiguration(ctx: EContext): String {
        return "INSERT INTO ${tableConfigurations(ctx)} (chain_iid, height, configuration_data) " +
                "VALUES (?, ?, ?) ON CONFLICT (chain_iid, height) DO UPDATE SET configuration_data = ?"
    }

    override fun cmdCreateTableGtxModuleVersion(ctx: EContext): String {
        return "CREATE TABLE ${tableGtxModuleVersion(ctx)} " +
                "(module_name TEXT PRIMARY KEY," +
                " version BIGINT NOT NULL)"
    }

    override fun insertBlock(ctx: EContext, height: Long): Long {
        val sql = "INSERT INTO ${tableBlocks(ctx)} (chain_iid, block_height) " +
                "VALUES (?, ?) RETURNING block_iid"

        return queryRunner.query(
                ctx.conn,
                sql,
                longRes,
                ctx.chainID,
                height)
    }

    override fun insertTransaction(ctx: BlockEContext, tx: Transaction): Long {
        val sql = "INSERT INTO ${tableTransactions(ctx)} (chain_iid, tx_rid, tx_data, tx_hash, block_iid) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING tx_iid"

        return queryRunner.query(
                ctx.conn,
                sql,
                longRes,
                ctx.chainID,
                tx.getRID(),
                tx.getRawData(),
                tx.getHash(),
                ctx.blockIID)
    }

    override fun addConfigurationData(ctx: EContext, height: Long, data: ByteArray) {
        queryRunner.insert(
                ctx.conn,
                cmdInsertConfiguration(ctx),
                longRes,
                ctx.chainID,
                height,
                data,
                data)
    }
}