// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx

import net.postchain.base.data.DatabaseAccess
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.core.EContext
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ScalarHandler
import org.apache.commons.lang3.text.StrSubstitutor
import java.util.*

// TODO: [POS-128]: Refactor SQL part of GTXModule
object GTXSchemaManager {

    private val queryRunner = QueryRunner()
    private val nullableLongRes = ScalarHandler<Long?>()

    // See SQLDatabaseAccess
    private fun tablePref(ctx: EContext): String {
        return "c${ctx.chainID}."
    }

    fun initializeDB(ctx: EContext) {
        val db = (DatabaseAccess.of(ctx) as SQLDatabaseAccess)
        if (!db.tableExists(ctx.conn, db.tableName(ctx, "gtx_module_version"))) {
            queryRunner.update(ctx.conn, db.cmdCreateTableGtxModuleVersion(ctx))
        }
    }

    fun getModuleVersion(ctx: EContext, name: String): Long? {
        val db = (DatabaseAccess.of(ctx) as SQLDatabaseAccess)
        val sql = "SELECT version FROM ${db.tableGtxModuleVersion(ctx)} WHERE module_name = ?"
        return queryRunner.query(ctx.conn, sql, nullableLongRes, name)
    }

    fun setModuleVersion(ctx: EContext, name: String, version: Long) {
        val db = (DatabaseAccess.of(ctx) as SQLDatabaseAccess)
        val currentVersion = getModuleVersion(ctx, name)

        if (currentVersion != null) {
            if (currentVersion != version) {
                val sql = "UPDATE ${db.tableGtxModuleVersion(ctx)} SET version = ? WHERE module_name = ?"
                queryRunner.update(ctx.conn, sql, name, version)
            }
        } else {
            val sql = "INSERT INTO ${db.tableGtxModuleVersion(ctx)} (module_name, version) VALUES (?, ?)"
            queryRunner.update(ctx.conn, sql, name, version)
        }
    }

    private fun loadModuleSQLSchema(ctx: EContext, jClass: Class<*>, name: String) {
        val sql = jClass.getResource(name).readText()

        // TODO: [POS-128]: Improve this
        val pref = tablePref(ctx)
        val tables = mapOf("pref" to pref)
        val sql2 = StrSubstitutor.replace(sql, tables)

        val schemaSQL = Scanner(sql2).useDelimiter("\\A").next()
        queryRunner.update(ctx.conn, schemaSQL)
    }

    fun autoUpdateSQLSchema(ctx: EContext,
                            schemaVersion: Int,
                            jClass: Class<*>,
                            schemaName: String,
                            moduleName: String? = null
    ) {
        val actualModuleName = moduleName ?: jClass.name
        val version = getModuleVersion(ctx, actualModuleName)
        if (version == null || version < schemaVersion) {
            loadModuleSQLSchema(ctx, jClass, schemaName)
            setModuleVersion(ctx, actualModuleName, schemaVersion.toLong())
        }
    }

}