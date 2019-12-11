package net.postchain.debug

enum class DiagnosticProperty(val prettyName: String) {

    VERSION("version"),

    PUB_KEY("pub-key"),

    BLOCKCHAIN("blockchain"),
    BLOCKCHAIN_RID("blockchain-rid"),
    BLOCKCHAIN_NODE_TYPE("node-type"),
    BLOCKCHAIN_NODE_PEERS("peers"),

    @Deprecated("POS-90")
    PEERS_TOPOLOGY("peers-topology")
}

enum class DpNodeType(val prettyName: String) {
    NODE_TYPE_VALIDATOR("Validator"),
    NODE_TYPE_REPLICA("Replica")
}