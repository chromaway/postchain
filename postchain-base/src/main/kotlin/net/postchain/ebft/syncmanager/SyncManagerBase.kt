// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.ebft.syncmanager

interface SyncManagerBase {
    val nodeStateTracker: NodeStateTracker
    fun update()
}
