// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

import mu.KLogging
import net.postchain.base.BaseAppContext
import net.postchain.base.BaseEContext
import net.postchain.base.Storage
import net.postchain.core.AppContext
import net.postchain.core.EContext
import net.postchain.core.ProgrammerMistake
import javax.sql.DataSource

class BaseStorage(
        private val readDataSource: DataSource,
        private val writeDataSource: DataSource,
        private val nodeId: Int,
        private val db: DatabaseAccess,
        private val savepointSupport: Boolean = true
) : Storage {

    companion object : KLogging()

    override fun newWritableContext(chainId: Long): EContext {
        return BaseEContext(writeDataSource.connection, chainId, nodeId, db)
    }

    override fun openReadConnection(): AppContext {
        val context = buildAppContext(readDataSource)
        if (!context.conn.isReadOnly) {
            throw ProgrammerMistake("Connection is not read-only")
        }
        return context
    }

    override fun closeReadConnection(context: AppContext) {
        if (!context.conn.isReadOnly) {
            throw ProgrammerMistake("Trying to close a writable connection as a read-only connection")
        }
        context.conn.close()
    }

    override fun openWriteConnection(): AppContext {
        return buildAppContext(writeDataSource)
    }

    override fun closeWriteConnection(context: AppContext, commit: Boolean) {
        with(context.conn) {
            when {
                isReadOnly -> throw ProgrammerMistake(
                        "Trying to close a read-only connection as a writeable connection")
                commit -> commit()
                else -> rollback()
            }

            close()
        }
    }

    override fun openReadConnection(chainID: Long): EContext {
        val context = buildEContext(chainID, readDataSource)
        if (!context.conn.isReadOnly) {
            throw ProgrammerMistake("Connection is not read-only")
        }
        return context
    }

    override fun closeReadConnection(context: EContext) {
        if (!context.conn.isReadOnly) {
            throw ProgrammerMistake("trying to close a writable connection as a read-only connection")
        }
        context.conn.close()
    }

    override fun openWriteConnection(chainID: Long): EContext {
        return buildEContext(chainID, writeDataSource)
    }

    override fun closeWriteConnection(context: EContext, commit: Boolean) {
        with(context.conn) {
            //            logger.debug("${context.nodeID} BaseStorage.closeWriteConnection()")
            when {
                isReadOnly -> throw ProgrammerMistake(
                        "trying to close a read-only connection as a writeable connection")
                commit -> commit()
                else -> rollback()
            }

            close()
        }
    }

    override fun isSavepointSupported(): Boolean = savepointSupport

    override fun withSavepoint(context: EContext, fn: () -> Unit): Exception? {
        var exception: Exception? = null

        val savepointName = "appendTx${System.nanoTime()}"
        val savepoint = context.conn.setSavepoint(savepointName)
        try {
            fn()
            context.conn.releaseSavepoint(savepoint)
        } catch (e: Exception) {
            logger.debug("Exception in savepoint $savepointName", e)
            context.conn.rollback(savepoint)
            exception = e
        }

        return exception
    }

    override fun close() {
        (readDataSource as? AutoCloseable)?.close()
        (writeDataSource as? AutoCloseable)?.close()
    }

    private fun buildAppContext(dataSource: DataSource): AppContext =
            BaseAppContext(dataSource.connection, db)

    private fun buildEContext(chainID: Long, dataSource: DataSource): EContext =
            BaseEContext(dataSource.connection, chainID, nodeId, db)
}
