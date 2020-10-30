// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import net.postchain.base.data.DatabaseAccess
import net.postchain.core.*
import java.sql.Connection

class ConfirmationProofMaterial(val txHash: ByteArrayKey,
                                val txHashes: Array<ByteArrayKey>,
                                val header: ByteArray,
                                val witness: ByteArray)

open class BaseAppContext(
        override val conn: Connection,
        private val dbAccess: DatabaseAccess
) : AppContext {

    override fun <T> getInterface(c: Class<T>): T? {
        return if (c == DatabaseAccess::class.java) {
            dbAccess as T?
        } else null
    }
}

open class BaseEContext(
        override val conn: Connection,
        override val chainID: Long,
        override val nodeID: Int,
        private val dbAccess: DatabaseAccess
) : EContext {

    override fun <T> getInterface(c: Class<T>): T? {
        return if (c == DatabaseAccess::class.java) {
            dbAccess as T?
        } else null
    }
}

open class BaseBlockEContext(
        val ectx: EContext,
        override val height: Long,
        override val blockIID: Long,
        override val timestamp: Long,
        val dependencyHeightMap: Map<Long, Long>
) : EContext by ectx, BlockEContext {

    /**
     * @param chainID is the blockchain dependency we want to look at
     * @return the required height of the blockchain (specificied by the chainID param)
     *         or null if there is no such dependency.
     *         (Note that Height = -1 is a dependency without any blocks, which is allowed)
     */
    override fun getChainDependencyHeight(chainID: Long): Long {
        return dependencyHeightMap[chainID]
                ?: throw IllegalArgumentException("The blockchain with chain ID: $chainID is not a dependency")
    }
}

open class BaseTxEContext(
        val bectx: BlockEContext,
        override val txIID: Long
) : BlockEContext by bectx, TxEContext