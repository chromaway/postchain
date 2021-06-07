// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.debug

enum class DiagnosticProperty(val prettyName: String) {

    VERSION("version"),

    PUB_KEY("pub-key"),

    BLOCKCHAIN("blockchain"),
    BLOCKCHAIN_RID("brid"),
    BLOCKCHAIN_NODE_TYPE("node-type"),
    BLOCKCHAIN_CURRENT_HEIGHT("height"),
    BLOCKCHAIN_NODE_PEERS("peers"),

    @Deprecated("POS-90")
    PEERS_TOPOLOGY("peers-topology")
}

enum class DpNodeType(val prettyName: String) {
    NODE_TYPE_SIGNER("signer"),
    NODE_TYPE_REPLICA("replica")
}