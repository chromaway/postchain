package com.chromaway.postchain.base.data

import com.chromaway.postchain.base.Storage
import com.chromaway.postchain.core.EContext
import com.chromaway.postchain.core.ProgrammerError
import javax.sql.DataSource

class BaseStorage(private val writeDataSource: DataSource, private val readDataSource: DataSource) : Storage {

//    private val writeDataSource: DataSource = writeDataSource
//    private val readDataSource: DataSource = readDataSource

    private fun getConnection(chainID: Int, dataSource: DataSource): EContext {
        val connection = dataSource.connection
        return EContext(connection, chainID)
    }

    override fun openReadConnection(chainID: Int): EContext {
        val eContext = getConnection(chainID, readDataSource)
        if (!eContext.conn.isReadOnly) {
            throw ProgrammerError("Connection is not read-only")
        }
        return eContext
    }

    override fun closeReadConnection(ectxt: EContext) {
        if (!ectxt.conn.isReadOnly) {
            throw ProgrammerError("trying to close a writable connection as a read-only connection")
        }
        ectxt.conn.close()
    }

    override fun openWriteConnection(chainID: Int): EContext {
        return getConnection(chainID, writeDataSource)
    }

    override fun closeWriteConnection(ectxt: EContext, commit: Boolean) {
        val conn = ectxt.conn
        if (conn.isReadOnly) {
            throw ProgrammerError("trying to close a read-only connection as a writeable connection")
        }
        if (commit) conn.commit() else conn.rollback()
        conn.close()
    }

    override fun withSavepoint(ctxt: EContext, fn: () -> Unit) {
        val savepointName = "appendTx${System.nanoTime()}"
        val savepoint = ctxt.conn.setSavepoint(savepointName)
        try {
            fn()
        } catch (e: Exception) {
            ctxt.conn.rollback(savepoint)
            throw e
        } finally {
            ctxt.conn.releaseSavepoint(savepoint)
        }
    }
}
