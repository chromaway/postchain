// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.ebft.syncmanager

import net.postchain.core.BlockDataWithWitness
import net.postchain.core.BlockchainConfiguration
import net.postchain.ebft.message.BlockData
import net.postchain.ebft.message.CompleteBlock

object BlockDataDecoder {

    fun decodeBlockDataWithWitness(block: CompleteBlock, blockchainConfig: BlockchainConfiguration)
            : BlockDataWithWitness {
        val header = blockchainConfig.decodeBlockHeader(block.data.header)
        val witness = blockchainConfig.decodeWitness(block.witness)
        return BlockDataWithWitness(header, block.data.transactions, witness)
    }

    fun decodeBlockData(block: BlockData, blockchainConfig: BlockchainConfiguration)
            : net.postchain.core.BlockData {
        val header = blockchainConfig.decodeBlockHeader(block.header)
        return net.postchain.core.BlockData(header, block.transactions)
    }
}