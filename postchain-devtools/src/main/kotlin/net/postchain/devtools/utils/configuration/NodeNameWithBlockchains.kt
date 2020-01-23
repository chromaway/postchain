// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.utils.configuration

data class NodeNameWithBlockchains(
        val nodeFileName: String,
        val blockchainFileAndId: List<NodeChain>
) {

    fun getFilenames(): List<String> = blockchainFileAndId.map { it.chainFilename }.toList()

    fun getChainIds(): List<Long> = blockchainFileAndId.map { it.chainId }.sorted().toList()

    // Will not return read only chains
    fun getWritableChainIds(): List<Long> {
        return blockchainFileAndId.filter { !it.readOnly }.map { it.chainId }.sorted().toList()
    }

    fun getReadOnlyChainIds(): List<Long> {
        return blockchainFileAndId.filter { it.readOnly }.map { it.chainId }.sorted().toList()
    }

    fun getNodeChain(chainId: Long): NodeChain? = blockchainFileAndId.find { it.chainId == chainId }

}

data class NodeChain(
        val chainFilename: String,
        val chainId: Long,
        val readOnly: Boolean = false // If the node uses this chain as read-only
)