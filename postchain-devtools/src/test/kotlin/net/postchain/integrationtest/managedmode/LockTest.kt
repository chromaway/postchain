// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.managedmode

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import mu.KLogging
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
//    @Test
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

//    @Test
    fun test2() {
        val gson = GsonBuilder().setPrettyPrinting().create()!!

        val obj = mapOf(
                "1" to mapOf(
                        "11" to "111>0",
                        "12" to "122<0"),
                "2" to mapOf(
                        "21" to "211>>0",
                        "22" to "222&0"),
                "3" to mapOf(
                        "31" to "311<0",
                        "32" to "322<111")
        )

        val json = JsonObject()
                .apply {
                    addProperty("version", "3.0.1")

                    obj.forEach { (property, value) ->
//                        addProperty(property, value.toString())
                        add(property, gson.toJsonTree(value))
                    }

                }.let(gson::toJson)

        println(json)
        println(obj)
    }
}