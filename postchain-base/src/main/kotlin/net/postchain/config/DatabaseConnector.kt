// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.config

import java.sql.Connection

interface DatabaseConnector {

    fun <Result> withReadConnection(action: (Connection) -> Result): Result

    fun <Result> withWriteConnection(action: (Connection) -> Result): Result

    fun openReadConnection(): Connection

    fun closeReadConnection(connection: Connection)

    fun openWriteConnection(): Connection

    fun closeWriteConnection(connection: Connection, commit: Boolean = true)
}