package net.postchain.integrationtest.managedmode

import mu.KLogging
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * LockTest -- to test simplified scheme of using shared lock by Node and Manager classes.
 * Will be deleted later.
 */

class Node {

    private val lock = ReentrantLock()
    val manager = Manager(lock)

    companion object : KLogging()

    fun reconfigure() {
        if (lock.tryLock()) {
            try {
                logger.info { "reconfigure: BEGIN" }
                Thread.sleep(3000)
                logger.info { "reconfigure: END" }
            } finally {
                lock.unlock()
            }
        } else {
            logger.error { "reconfigure: can't acquire a lock" }
        }
    }
}

class Manager(val lock: Lock) {

    companion object : KLogging()

    fun reconfigure() {
        lock.withLock {
            logger.info { "reconfigure: BEGIN" }
            Thread.sleep(5000)
            logger.info { "reconfigure: END" }
        }
    }
}

class LockTest {

    /**
     * Log:

    INFO  2019-10-04 17:44:29.872 [main] Manager - reconfigure: BEGIN
    INFO  2019-10-04 17:44:31.868 [pool-1-thread-1] Node - reconfigure: can't acquire a lock
    INFO  2019-10-04 17:44:33.870 [pool-1-thread-1] Node - reconfigure: can't acquire a lock
    INFO  2019-10-04 17:44:34.881 [main] Manager - reconfigure: END
    INFO  2019-10-04 17:44:34.881 [main] Manager - reconfigure: BEGIN
    INFO  2019-10-04 17:44:35.871 [pool-1-thread-1] Node - reconfigure: can't acquire a lock
    INFO  2019-10-04 17:44:37.872 [pool-1-thread-1] Node - reconfigure: can't acquire a lock
    INFO  2019-10-04 17:44:39.873 [pool-1-thread-1] Node - reconfigure: can't acquire a lock
    INFO  2019-10-04 17:44:39.882 [main] Manager - reconfigure: END
    INFO  2019-10-04 17:44:41.875 [pool-1-thread-1] Node - reconfigure: BEGIN
    INFO  2019-10-04 17:44:44.875 [pool-1-thread-1] Node - reconfigure: END

    Process finished with exit code 0

     */
    @Test
    fun test() {
        val executor = Executors.newSingleThreadScheduledExecutor()
        val node = Node()

        executor.scheduleWithFixedDelay(
                {
                    node.reconfigure()
                },
                2, 2, TimeUnit.SECONDS)

        node.manager.reconfigure()
        node.manager.reconfigure()

        Thread.sleep(5000)

        executor.shutdownNow()
        executor.awaitTermination(1000, TimeUnit.SECONDS)
    }
}