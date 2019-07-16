package net.postchain.ebft.syncmanager.replica

import mu.KLogging
import net.postchain.ebft.syncmanager.StatusLogInterval
import java.util.*


class ReplicaTelemetry(private val blockchainProcessName: String) {
    companion object : KLogging()

    enum class LogLevel {
        Error, Warning, Debug
    }

    private fun consoleLogger(level: LogLevel, message: String, throwable: Throwable? = null) {
        val msg = "[$blockchainProcessName]: $message"

        when (level) {
            LogLevel.Error -> {
                if (throwable != null) {
                    logger.error(msg, throwable)
                } else {
                    logger.error(msg)
                }
            }
            LogLevel.Warning -> {
                logger.warn(msg)
            }
            LogLevel.Debug -> {
                logger.debug(msg)
            }
        }
    }

    private var lastLoggedTimestamp = 0L

    fun logCurrentState(blockHeight: Long, parallelRequestsState: HashMap<Long, IssuedRequestTimer>, blocks: PriorityQueue<IncomingBlock>) {
        val now = Date().time
        if (now > lastLoggedTimestamp + StatusLogInterval) {
            lastLoggedTimestamp = now
            val buffer = blocks.map { it.height }.toString()
            val requestsState = parallelRequestsState.map { "${it.key} last sent: ${it.value.lastSentTimestamp}" }.toString()
            consoleLogger(LogLevel.Debug, "[he:$blockHeight | buffer:$buffer | active requests:$requestsState]")
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
