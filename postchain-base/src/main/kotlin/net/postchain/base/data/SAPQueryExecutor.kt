package net.postchain.base.data

import net.postchain.core.EContext
import java.sql.PreparedStatement

object SAPQueryExecutor {

    fun executeByteArrayQuery(ctx: EContext, query: String, argumentator: (PreparedStatement) -> Unit): ByteArray? {
        val statement = ctx.conn.prepareStatement(query)
                .apply(argumentator)

        val resultSet = statement.executeQuery()

        return if (resultSet.next()) {
            val blob = resultSet.getBlob(1)
            blob.binaryStream.readBytes()
        } else {
            null
        }
    }

    fun executeByteArrayListQuery(ctx: EContext, query: String, argumentator: (PreparedStatement) -> Unit): List<ByteArray> {
        val statement = ctx.conn.prepareStatement(query)
                .apply(argumentator)

        val resultSet = statement.executeQuery()
        val result = mutableListOf<ByteArray>()

        while (resultSet.next()) {
            val blob = resultSet.getBlob(1)
            result.add(blob.binaryStream.readBytes())
        }

        return result
    }
}