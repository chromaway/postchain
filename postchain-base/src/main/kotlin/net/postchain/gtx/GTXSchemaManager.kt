// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.gtx

import net.postchain.base.data.DatabaseAccess
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.core.EContext
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ScalarHandler
import java.util.*

object GTXSchemaManager {
    private val r = QueryRunner()
    private val nullableLongRes = ScalarHandler<Long?>()

    fun initializeDB(ctx: EContext) {
        // if creating table has exception, it means table exists
        try {
            val db: SQLDatabaseAccess = DatabaseAccess.of(ctx) as SQLDatabaseAccess
            val sqlCommands = db.sqlCommands
            r.update(ctx.conn, sqlCommands.createTableGtxModuleVersion)
        } catch (e : Exception) {

        }
    }

    fun getModuleVersion(ctx: EContext, name: String): Long? {
        return r.query(ctx.conn,
                "SELECT version FROM gtx_module_version WHERE module_name = ?",
                nullableLongRes, name)
    }

    fun setModuleVersion(ctx: EContext, name: String, version: Long) {
        val oldversion = getModuleVersion(ctx, name)

        if (oldversion != null) {
            if (oldversion != version) {
                r.update(ctx.conn,
                        """UPDATE gtx_module_version SET version = ? WHERE module_name = ?""",
                        name, version)
            }
        } else {
            r.update(ctx.conn,
                    """INSERT INTO gtx_module_version (module_name, version)
                    VALUES (?, ?)""",
                    name, version)
        }
    }

    fun loadModuleSQLSchema(ctx: EContext, jclass: Class<*>, name: String) {
        val r = QueryRunner()
        val schemaSQL = Scanner(jclass.getResourceAsStream(name), "UTF-8").useDelimiter("\\A").next()
        r.update(ctx.conn, schemaSQL)
    }

    fun autoUpdateSQLSchema(ctx: EContext,
                            schemaVersion: Int,
                            jclass: Class<*>,
                            schemaName: String,
                            moduleName: String? = null)
    {
        val actualModuleName = moduleName ?: jclass.name
        val version = getModuleVersion(ctx, actualModuleName)
        if (version == null || version < schemaVersion) {
            loadModuleSQLSchema(ctx, jclass, schemaName)
            setModuleVersion(ctx, actualModuleName, schemaVersion.toLong())
        }
    }

}