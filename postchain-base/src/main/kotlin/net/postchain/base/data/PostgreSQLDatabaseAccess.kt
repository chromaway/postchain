package net.postchain.base.data

import net.postchain.core.BlockEContext
import net.postchain.core.EContext
import net.postchain.core.Transaction

class PostgreSQLDatabaseAccess(sqlCommands: PostgreSQLCommands) : SQLDatabaseAccess(sqlCommands) {

    override fun insertBlock(ctx: EContext, height: Long): Long {
        return queryRunner.insert(ctx.conn, "INSERT INTO blocks (chain_id, block_height) VALUES (?, ?) RETURNING block_iid", longRes, ctx.chainID, height)
    }

    override fun insertTransaction(ctx: BlockEContext, tx: Transaction): Long {
        return queryRunner.insert(ctx.conn,
                "INSERT INTO transactions (chain_id, tx_rid, tx_data, tx_hash, block_iid) VALUES (?, ?, ?, ?, ?) RETURNING tx_iid",
                longRes,
                ctx.chainID, tx.getRID(), tx.getRawData(), tx.getHash(), ctx.blockIID)
    }
}