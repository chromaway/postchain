package net.postchain.debug

enum class DiagnosticProperty(val prettyName: String) {

    VERSION("version"),

    PUB_KEY("pub-key"),

    BLOCKCHAIN("blockchain"),
    BLOCKCHAIN_RID("blockchain-rid"),
    BLOCKCHAIN_NODE_TYPE("blockchain-node-type"),

    PEERS_TOPOLOGY("peers-topology")
}