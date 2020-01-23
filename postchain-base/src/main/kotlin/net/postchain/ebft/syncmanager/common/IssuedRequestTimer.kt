// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.syncmanager.common

data class IssuedRequestTimer(val backoffDelta: Int, val lastSentTimestamp: Long)
