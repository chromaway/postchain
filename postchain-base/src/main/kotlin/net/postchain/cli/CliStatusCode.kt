// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.cli

import java.sql.SQLException

sealed class CliResult(open val code: Int = -1)
data class Ok(val info: String? = null, override val code: Int = 0, val isLongRunning: Boolean = false) : CliResult()

sealed class CliError(open val message: String? = null) : CliResult() {

    data class DatabaseError(
            override val code: Int = 1,
            override val message: String? = "Database error"
    ) : CliError() {
        constructor(e: SQLException) : this(1, "Database error: " +
                "sql-state: ${e.sqlState}, error-code: ${e.errorCode}, message: ${e.message}"
        )
    }

    data class DatabaseOffline(
            override val code: Int = 2,
            override val message: String? = "Unable to connect to database") : CliError()

    data class ArgumentNotFound(
            override val code: Int = 3,
            override val message: String? = "Argument not found", val command: String) : CliError()

    data class MissingCommand(
            override val code: Int = 4,
            override val message: String?
    ) : CliError() {
        constructor(command: String) : this(4, "Command not found: $command")
    }

    data class CommandNotAllowed(
            override val code: Int = 5,
            override val message: String?) : CliError()

    data class NotImplemented(
            override val code: Int = 6,
            override val message: String?) : CliError()

    data class CheckBlockChain(
            override val code: Int = 7,
            override val message: String?) : CliError()

    companion object {
        open class CliException(override val message: String, override val cause: Exception? = null) : RuntimeException(message, cause)
    }
}
