package net.postchain.integrationtest.sync

import mu.KLogging
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SyncTest(val signerCount: Int, val replicaCount: Int, val syncIndex: Set<Int>, val stopIndex: Set<Int>, val blocksToSync: Int) : AbstractSyncTest() {

    private companion object : KLogging() {

        @JvmStatic
        @Parameterized.Parameters
        fun testArguments() = listOf(
                // Single block test
                arrayOf(1, 1, setOf(0), setOf<Int>(), 1),
                arrayOf(1, 1, setOf(1), setOf<Int>(), 1),
                arrayOf(1, 2, setOf(1), setOf<Int>(0), 1),
                arrayOf(2, 0, setOf(1), setOf<Int>(), 1),

                // Multi block test
                arrayOf(1, 1, setOf(0), setOf<Int>(), 50),
                arrayOf(1, 1, setOf(1), setOf<Int>(), 50),
                arrayOf(1, 2, setOf(1), setOf<Int>(0), 50),
                arrayOf(2, 0, setOf(1), setOf<Int>(), 50),

                // Multi node multi blocks
                arrayOf(4, 4, setOf(0, 1, 2, 4, 5), setOf<Int>(3, 6), 50)
        )
    }

    @Test
    fun sync() {
        doStuff(signerCount, replicaCount, syncIndex, stopIndex, blocksToSync)
    }
}