// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

import net.postchain.core.BlockEContext
import net.postchain.core.EContext
import net.postchain.core.Transaction

class PostgreSQLDatabaseAccess(sqlCommands: SQLCommands) : SQLDatabaseAccess(sqlCommands) {

    override fun insertBlock(ctx: EContext, height: Long): Long {
        return queryRunner.query(
                ctx.conn,
                "INSERT INTO blocks (chain_iid, block_height) VALUES (?, ?) RETURNING block_iid",
                longRes,
                ctx.chainID,
                height)
    }

    override fun insertTransaction(ctx: BlockEContext, tx: Transaction): Long {
        return queryRunner.query(
                ctx.conn,
                "INSERT INTO transactions (chain_iid, tx_rid, tx_data, tx_hash, block_iid) VALUES (?, ?, ?, ?, ?) RETURNING tx_iid",
                longRes,
                ctx.chainID,
                tx.getRID(),
                tx.getRawData(),
                tx.getHash(),
                ctx.blockIID)
    }

    override fun addConfigurationData(context: EContext, height: Long, data: ByteArray) {
        queryRunner.insert(
                context.conn,
                sqlCommands.insertConfiguration,
                longRes,
                context.chainID,
                height,
                data,
                data)
    }
}