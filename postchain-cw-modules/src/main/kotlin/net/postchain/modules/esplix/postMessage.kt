package net.postchain.modules.esplix

import net.postchain.core.TxEContext
import net.postchain.gtx.ExtOpData
import net.postchain.gtx.GTXOperation
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.ScalarHandler

class post_message_op (val config: EsplixConfig, data: ExtOpData): GTXOperation(data) {
    val prevID = data.args[0].asByteArray()
    val payload = data.args[1].asByteArray()
    val signers = data.signers.reduce{it, acc -> it + acc}
    val messageID = config.cryptoSystem.digest(prevID+payload+signers)

    private val r = QueryRunner()
    private val unitHandler = ScalarHandler<Unit>()

    override fun isCorrect(): Boolean {
        if (data.args.size != 2)
            return false
        return true
    }

    override fun apply(ctx: TxEContext): Boolean {
        r.query(ctx.conn, "SELECT mcs_r2_postMessage(?, ?, ?, ?, ?)", unitHandler,
                ctx.txIID, data.opIndex, messageID, prevID, payload)
        return true
    }
}
