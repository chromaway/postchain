package net.postchain.integrationtest.sync

import mu.KLogging
import net.postchain.devtools.currentHeight
import org.awaitility.Awaitility
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/* One signer, two replica nodes. After one block, node 0 (signer is turned off).
 * Node 1 (a replica) is wiped. Need node 2 (the other replica) to be able to sync.
 */
class MustSyncUntilTest : AbstractSyncTest() {

    private companion object: KLogging()
    val chainID: Long =  0L //In AbstractSyncTest, chainID is hard coded to 0L.
    override var mustSyncUntil = 1L //default value is -1
    val signers = 2
    val replicas = 0
    val blocksToSync = 2
    val syncNodeIndex = 0


    /* Try to synchronize to a height that does not yet exist.
    Check height explicitly for sync node, to assert that it has height = syncUntilHeight
    */
    @Test
    fun testSyncUntilNonExistingHeight() {
        mustSyncUntil = 3L
        try {
            Awaitility.await().atMost(9, TimeUnit.SECONDS).until {
                runSyncTest(signers, replicas, setOf(syncNodeIndex), setOf(), blocksToSync)
                true
            }
        } catch (e: org.awaitility.core.ConditionTimeoutException) {
            val actual = nodes[syncNodeIndex].currentHeight(chainID)
            assertEquals((blocksToSync-1).toLong(), actual)
        }
    }

    // Assert when height mustSyncUntil exist.
    @Test
    fun testSyncUntilHeight() {
        mustSyncUntil = 1L
        runSyncTest(signers, replicas, setOf(syncNodeIndex), setOf(), blocksToSync)
    }
}