package net.postchain.ebft.syncmanager.fastsync

data class IssuedRequestTimer(val backoffDelta: Int, val lastSentTimestamp: Long)
