// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.configurations

import net.postchain.core.EContext
import net.postchain.core.TxEContext
import net.postchain.core.UserMistake
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtx.ExtOpData
import net.postchain.gtx.GTXOperation
import net.postchain.gtx.GTXSchemaManager
import net.postchain.gtx.SimpleGTXModule
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ScalarHandler

private val r = QueryRunner()
private val nullableStringReader = ScalarHandler<String?>()

class GTXTestOp(u: Unit, opdata: ExtOpData) : GTXOperation(opdata) {

    /**
     * The only way for the [GtxTestOp] to be considered correct is if first argument is "1" and the second is a string.
     */
    override fun isCorrect(): Boolean {
        if (data.args.size != 2) return false
        data.args[1].asString()
        return data.args[0].asInteger() == 1L
    }

    override fun apply(ctx: TxEContext): Boolean {
        if (data.args[1].asString() == "rejectMe")
            throw UserMistake("You were asking for it")
        r.update(ctx.conn,
                """INSERT INTO gtx_test_value(tx_iid, value) VALUES (?, ?)""",
                ctx.txIID, data.args[1].asString())
        return true
    }
}

class GTXTestModule : SimpleGTXModule<Unit>(Unit,
        mapOf("gtx_test" to ::GTXTestOp),
        mapOf("gtx_test_get_value" to { u, ctxt, args ->
            val txRID = (args as GtvDictionary).get("txRID")
            if (txRID == null) {
                throw UserMistake("No txRID property supplied")
            }

            val value = r.query(ctxt.conn,
                    """SELECT value FROM gtx_test_value
                    INNER JOIN transactions ON gtx_test_value.tx_iid = transactions.tx_iid
                    WHERE transactions.tx_rid = ?""",
                    nullableStringReader, txRID.asByteArray(true))
            if (value == null)
                GtvNull
            else
                gtv(value)
        })
) {
    override fun initializeDB(ctx: EContext) {
        val moduleName = this::class.qualifiedName!!
        val version = GTXSchemaManager.getModuleVersion(ctx, moduleName)
        if (version == null) {
            r.update(ctx.conn, """
CREATE TABLE gtx_test_value(tx_iid BIGINT PRIMARY KEY, value TEXT NOT NULL)
            """)
            GTXSchemaManager.setModuleVersion(ctx, moduleName, 0)
        }
    }
}
