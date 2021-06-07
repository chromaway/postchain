package net.postchain.ebft.syncmanager.common

import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PeerStatusesKnownStateTest {

    /**
     * This is a scenario where blacklist a peer, make it time out, and blacklist it again later.
     */
    @Test
    fun test_blacklist_and_timeout () {
        val params = FastSyncParameters()
        val state = PeerStatuses.KnownState(params)

        // Initial state
        assertTrue(state.isSyncable(1))

        var timeIter = makePeerBlacklisted(state)
        assertFalse(state.isSyncable(1)) //
        assertTrue(state.isBlacklisted(timeIter)) // 10 blacklist calls should make it blacklisted
        val expectedTimeout = params.blacklistingTimeoutMs + timeIter // This is when we will get it back

        // Wait one millisecond
        assertTrue(state.isBlacklisted(timeIter + 1))

        // Wait some time
        val wait = params.blacklistingTimeoutMs / 2
        assertTrue(state.isBlacklisted(timeIter + wait)) // Still blacklisted

        // Wait until the exact right time for release
        assertFalse(state.isBlacklisted( expectedTimeout + 1)) // Now released from blacklist
        assertTrue(state.isSyncable(1)) // Syncable again

        // We have to make sure it works again, but at some later time
        val currentTime = timeIter + expectedTimeout * 3;
        timeIter = makePeerBlacklisted(state, currentTime)
        assertFalse(state.isSyncable(1))
        assertTrue(state.isBlacklisted(timeIter))

    }

    private fun makePeerBlacklisted(state: PeerStatuses.KnownState, startTime: Long = 0L): Long {
        // Work up until blacklist
        var timeIter: Long = 1 + startTime
        while (timeIter < (10 + startTime)) {
            state.blacklist("Olle doesn't like this peer", timeIter)
            assertTrue(state.isSyncable(1))
            timeIter++
        }

        // Last straw that breaks the camel's back
        state.blacklist("Olle REALLY doesn't like this peer", timeIter)
        return timeIter
    }
}