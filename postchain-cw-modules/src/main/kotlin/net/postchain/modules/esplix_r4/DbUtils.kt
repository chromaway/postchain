package net.postchain.modules.esplix_r4

import net.postchain.core.EContext

object DbUtils {

    fun tablePref(ctx: EContext): String {
        return "c${ctx.chainID}."
    }

    fun tableName(ctx: EContext, table: String): String {
        return "\"c${ctx.chainID}.${table}\""
    }
}