// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config.node

import com.nhaarman.mockitokotlin2.mock
import net.postchain.config.DatabaseConnector
import java.sql.Connection

@Deprecated("POS-128")
class MockDatabaseConnector : DatabaseConnector {

    override fun <Result> withReadConnection(action: (Connection) -> Result): Result = action(mock())

    override fun <Result> withWriteConnection(action: (Connection) -> Result): Result = action(mock())

    override fun openReadConnection(): Connection = mock()

    override fun closeReadConnection(connection: Connection) = Unit

    override fun openWriteConnection(): Connection = mock()

    override fun closeWriteConnection(connection: Connection, commit: Boolean) = Unit
}