package net.postchain.base

import java.util.concurrent.CountDownLatch

typealias PeerID = ByteArray

open class PeerInfo(val host: String, open val port: Int, val pubKey: ByteArray)

class DynamicPortPeerInfo(host: String, pubKey: ByteArray) : PeerInfo(host, 0, pubKey) {
    private val latch = CountDownLatch(1)
    private var assignedPortNumber = 0

    override val port: Int
        get() {
            latch.await()
            return assignedPortNumber
        }

    fun portAssigned(port: Int) {
        assignedPortNumber = port
        latch.countDown()
    }
}
