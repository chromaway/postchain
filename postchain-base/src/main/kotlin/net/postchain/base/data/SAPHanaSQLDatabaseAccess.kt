package net.postchain.base.data

import net.postchain.core.EContext

class SAPHanaSQLDatabaseAccess(sqlCommands: SQLCommands) : SQLDatabaseAccess(sqlCommands) {

    override fun getTxBytes(ctx: EContext, txRID: ByteArray): ByteArray? {
        val statement = ctx.conn.prepareStatement("SELECT tx_data FROM transactions WHERE chain_id=? AND tx_rid=?")
                .apply {
                    setLong(1, ctx.chainID)
                    setBytes(2, txRID)
                }

        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val blob = resultSet.getBlob(1)
            blob.binaryStream.readBytes()
        } else {
            null
        }
    }
}