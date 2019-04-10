package net.postchain.ebft.syncmanager.replica

import mu.KLogging
import java.util.*


class ReplicaTelemetry() {
    companion object : KLogging()

    enum class LogLevel {
        Error, Warning, Debug
    }

    private fun consoleLogger(level: LogLevel, message: String, throwable: Throwable? = null) = when (level) {
        LogLevel.Error -> {
            if (throwable != null) {
                logger.error(message, throwable)
            } else logger.error(message)
        }
        LogLevel.Warning -> {
            logger.warn(message)
        }
        LogLevel.Debug -> {
            logger.debug(message)
        }
    }

    private var lastLoggedTimestamp = 0L
    private var backoffTimeMs = 1000L

    fun logCurrentState(blockHeight: Long, parallelRequestsState: HashMap<Long, IssuedRequestTimer>, blocks: PriorityQueue<IncomingBlock>) {
        val now = Date().time
        if (now > lastLoggedTimestamp + backoffTimeMs) {
            lastLoggedTimestamp = now
            val buffer = blocks.map { it.height }.toString()
            val requestsState = parallelRequestsState.map { "${it.key} last sent: ${it.value.lastSentTimestamp}" }.toString()
            consoleLogger(LogLevel.Debug, "Replica [H: $blockHeight | buffer: $buffer | active requests: $requestsState]")
        }
    }

    fun blockAppendedToDatabase(height: Long) {
        consoleLogger(LogLevel.Debug, "Appended block [$height]")
    }

    fun failedToAppendBlockToDatabase(height: Long, message: String?) {
        consoleLogger(LogLevel.Error, "Failed to add block [$height]: $message")
    }

    fun askForBlock(height: Long, blockHeight: Long) {
        consoleLogger(LogLevel.Debug, "Ask for block: $height | current height: $blockHeight")
    }

    fun fatal(message: String, throwable: Throwable) {
        consoleLogger(LogLevel.Error, message, throwable)
    }
}
