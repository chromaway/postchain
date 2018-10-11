package net.postchain.base

import net.postchain.core.EContext

/**
 * Handles back-end database connection and storage
 */
interface Storage {
    fun openReadConnection(chainID: Long): EContext
    fun closeReadConnection(context: EContext)

    fun openWriteConnection(chainID: Long): EContext
    fun closeWriteConnection(context: EContext, commit: Boolean)

    fun withSavepoint(context: EContext, fn: () -> Unit): Exception?

    fun close()
}