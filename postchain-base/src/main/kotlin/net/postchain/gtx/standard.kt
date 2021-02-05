// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx

import mu.KLogging
import net.postchain.base.data.DatabaseAccess
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.core.EContext
import net.postchain.core.TxEContext
import net.postchain.gtv.*
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import org.apache.commons.dbutils.handlers.ScalarHandler

/**
 * nop operation can be useful as nonce or identifier which has no meaning on consensus level
 */
class GtxNop(u: Unit, opData: ExtOpData) : GTXOperation(opData) {

    companion object : KLogging() {
        const val OP_NAME = "nop"
        private const val MAX_SIZE = 64 // Number of bytes/chars we allow per argument
    }

    override fun apply(ctx: TxEContext): Boolean {
        return true
    }

    /**
     * A Nop is correct if
     * 1. It has not more than one argument
     * 2. Argument is of type GtvNull, GtvString, GtvInteger or GtvByteArray
     * 3. Argument is < 64 bytes
     */
    override fun isCorrect(): Boolean {
        if (data.args.size > 1) return false
        if (data.args.isEmpty()) return true
        // Validation: To prevent spam from entering the BC we validate the argument
        return validateArgument(data.args[0])
    }

    /**
     * Validates the nop argument, making sure it is simple and not too big.
     */
    private fun validateArgument(arg: Gtv): Boolean {
        return when (arg) {
            is GtvNull -> true
            is GtvInteger -> checkSize(arg.nrOfBytes(), "GtvInteger")
            is GtvByteArray -> checkSize(arg.nrOfBytes(), "GtvByteArray")
            is GtvString -> checkSize(arg.asString().length, "GtvString")
            else -> {
                if (GtxNop.logger.isTraceEnabled) {
                    GtxNop.logger.trace("Argument of type: ${arg.type} not allowed for operation: $OP_NAME. ")
                }
                false
            }
        }
    }

    private fun checkSize(size: Int, type: String): Boolean {
        return if (size <= MAX_SIZE) {
            true
        } else {
            if (GtxNop.logger.isTraceEnabled) {
                GtxNop.logger.trace("Argument of type: $type too big. Max: $MAX_SIZE chains but was $size (operation: $OP_NAME) ")
            }
            false
        }
    }
}

/**
 * Will take one or two arguments "from" and "to".
 * If current timestamp in within this interval we return true.
 */
class GtxTimeB(u: Unit, opData: ExtOpData) : GTXOperation(opData) {

    companion object {
        const val OP_NAME = "timeb"
    }

    /**
     * 1. Nof args must be two
     * 2. If arg1 is not null, it must be less than arg0
     */
    override fun isCorrect(): Boolean {
        if (data.args.size != 2) return false
        val from = data.args[0].asInteger()
        if (!data.args[1].isNull()) {
            if (data.args[1].asInteger() < from) return false
        }
        return true
    }

    override fun apply(ctx: TxEContext): Boolean {
        val from = data.args[0].asInteger()
        if (ctx.timestamp < from) return false
        if (!data.args[1].isNull()) {
            val until = data.args[1].asInteger()
            if (until < ctx.timestamp) return false
        }
        return true
    }

}

fun lastBlockInfoQuery(config: Unit, ctx: EContext, args: Gtv): Gtv {
    val dba = DatabaseAccess.of(ctx) as SQLDatabaseAccess
    val prevHeight = dba.getLastBlockHeight(ctx)
    val prevTimestamp = dba.getLastBlockTimestamp(ctx)
    val prevBlockRID: ByteArray?
    if (prevHeight != -1L) {
        prevBlockRID = dba.getBlockRID(ctx, prevHeight)!!
    } else {
        prevBlockRID = null
    }
    return gtv(
            "height" to gtv(prevHeight),
            "timestamp" to gtv(prevTimestamp),
            "blockRID" to (if (prevBlockRID != null) gtv(prevBlockRID) else GtvNull)
    )
}

fun txConfirmationTime(config: Unit, ctx: EContext, args: Gtv): Gtv {
    val dba : SQLDatabaseAccess = DatabaseAccess.of(ctx) as SQLDatabaseAccess
    val argsDict = args as GtvDictionary
    val txRID = argsDict["txRID"]!!.asByteArray(true)
    val info = dba.getBlockInfo(ctx, txRID)
    val timestamp = dba.queryRunner.query(ctx.conn,"SELECT timestamp FROM blocks WHERE block_iid = ?",
            ScalarHandler<Long>(), info.blockIid)
    val blockRID = dba.queryRunner.query(ctx.conn,"SELECT block_rid FROM blocks WHERE block_iid = ?",
            ScalarHandler<ByteArray>(), info.blockIid)
    val blockHeight = dba.queryRunner.query(ctx.conn,"SELECT block_height FROM blocks WHERE block_iid = ?",
            ScalarHandler<Long>(), info.blockIid)
    return gtv(
            "timestamp" to gtv(timestamp),
            "blockRID" to gtv(blockRID),
            "blockHeight" to gtv(blockHeight)
    )
}


class StandardOpsGTXModule : SimpleGTXModule<Unit>(Unit, mapOf(
        GtxNop.OP_NAME to ::GtxNop,
        GtxTimeB.OP_NAME to ::GtxTimeB
), mapOf(
        "last_block_info" to ::lastBlockInfoQuery,
        "tx_confirmation_time" to ::txConfirmationTime
        )
) {
    override fun initializeDB(ctx: EContext) {}
}