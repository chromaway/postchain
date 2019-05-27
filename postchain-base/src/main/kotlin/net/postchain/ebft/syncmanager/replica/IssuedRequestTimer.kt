package net.postchain.ebft.syncmanager.replica

data class IssuedRequestTimer(val backoffDelta: Int, val lastSentTimestamp: Long)
