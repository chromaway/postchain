package net.postchain.ebft.syncmanager.common

data class IssuedRequestTimer(val backoffDelta: Int, val lastSentTimestamp: Long)
