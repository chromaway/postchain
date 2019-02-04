package net.postchain.cli

sealed class CliResult(open val code: Int = -1)
data class Ok(val info: String? = null, override val code: Int = 0) : CliResult()

sealed class CliError(open val message: String? = null) : CliResult() {

    data class DatabaseOffline(
            override val code: Int = 1,
            override val message: String? = "Unable to connect to database") : CliError()
    data class CommandNotFound(
            override val code: Int = 2,
            override val message: String? = "Command not found", val command: String) : CliError()
    data class MissingCommand(
            override val code: Int = 3,
            override val message: String?) : CliError()
    data class CommandNotAllowed(
            override val code: Int = 4,
            override val message: String?) : CliError()
    data class NotImplemented(
            override val code: Int = 5,
            override val message: String?) : CliError()
    data class CheckBlockChain(
            override val code: Int = 6,
            override val message: String?) : CliError()

    companion object {
        open class CliException(override val message: String, override val cause: Exception? = null) : RuntimeException(message, cause)
    }
}
