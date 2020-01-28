// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

class NodeStateTracker {
    var nodeStatuses: Array<String>? = null
    var myStatus: String? = null
    var blockHeight: Long = -1
}
