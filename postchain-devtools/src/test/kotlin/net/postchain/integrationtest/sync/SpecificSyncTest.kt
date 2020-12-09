package net.postchain.integrationtest.sync

import mu.KLogging
import net.postchain.devtools.awaitHeight
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/*
 When a node (old or new) comes online it might not become aware of any currently
 active nodes because its nodelist might be outdated (network has evolved over time).

 It doesn’t have to happen at height -1. This can happen at any height
 when the node has been offline for a while. This can only happen on chain0,
 because that's where all blockchain configurations and nodes are being tracked.
 As long as chain0 is fully synced, a node should be able to sync any other blockchain
 in that domain, because it has the most recent signerlist. Postchain in managed
 mode should therefore make sure that chain0 is synced before starting to sync its
 managed blockchains.

 Non-obvious tests to implement:

 1 Sync a managed blockchain (not chain0) from final nodes, where none of the initial nodes exist. (Should work fine)
 2 Sync chain0 from newest nodes, where none of the initial nodes exist. Should work because newest nodes know about me and I'm signer.
 3 Test 2, but give it a syncnode via configuration (Should work)
 4 Sync single sole node from single nodelist (should wait for discovery timeout)

  */
class SpecificSyncTest : AbstractSyncTest() {

    private companion object: KLogging()

    @Test
    fun testSyncManagedBlockchain() {
        // We will sync a managed blockchain where I'm not an initial signer, but
        // I am an initial signer in the last configuration.

        // Start one single signer node 0
        // Build 25 blocks
        // Add blockchain 100 (with only signer 1)
        // Build 25 blocks on bc100

        // Start node 1
        // It should sync chain0 and then discover chain 100 and sync that too.
    }
}