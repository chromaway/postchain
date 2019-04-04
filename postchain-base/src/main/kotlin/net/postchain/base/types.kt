// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.base

import net.postchain.base.data.DatabaseAccess
import net.postchain.core.BlockEContext
import net.postchain.core.EContext
import net.postchain.core.TxEContext
import java.sql.Connection

class ConfirmationProofMaterial(val txHash: ByteArray,
                                val txHashes: Array<ByteArray>,
                                val header: ByteArray,
                                val witness: ByteArray)


open class BaseEContext(
        override val conn: Connection,
        override val chainID: Long,
        override val nodeID: Int,
        val dbAccess: DatabaseAccess
) : EContext {

    override fun <T> getInterface(c: Class<T>): T? {
        if (c == DatabaseAccess::class.java) {
            return dbAccess as T?
        } else return null
    }
}

open class BaseBlockEContext(
        val ectx: EContext,
        override val blockIID: Long,
        override val timestamp: Long,
        val dependencyHeightMap: Map<Long, Long>
) : EContext by ectx, BlockEContext {

    /**
     * @param chainID is the blockchain dependency we want to look at
     * @return the required height of the blockchain (specificied by the chainID param)
     *         or null if there is no such dependency.
     *         (Note that Height = 0 is a dependency without any blocks, which is allowed)
     */
    fun getChainDependencyHeight(chainID:Long): Long? {
        return dependencyHeightMap[chainID]
    }
}

open class BaseTxEContext(
        val bectx: BlockEContext,
        override val txIID: Long
) : BlockEContext by bectx, TxEContext