// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.modules.ft

import net.postchain.core.EContext
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull

/**
 * Query that checks if an account exists given the account identifier
 *
 * @param config configuration for the FT module
 * @param ctx contextual information
 * @param args arguments including account identifier and asset identifier
 * @return Gtv of 1L for true and 0L for false
 */
fun ftAccountExistsQ(config: FTConfig, ctx: EContext, args: Gtv): Gtv {
    val accountID = args["account_id"]!!.asByteArray(true)
    val exists = if (config.dbOps.getDescriptor(ctx, accountID) != null) 1L else 0L
    return gtv("exists" to gtv(exists))
}

/**
 * Query that returns the balance of a given account for a given asset
 *
 * @param config configuration for the FT module
 * @param ctx contextual information
 * @param args arguments including account identifier and asset identifier
 * @return the balance of the account
 */
fun ftBalanceQ(config: FTConfig, ctx: EContext, args: Gtv): Gtv {
    val accountID = args["account_id"]!!.asByteArray(true)
    val assetID = args["asset_id"]!!.asString()
    return gtv(
            "balance" to
                    gtv(config.dbOps.getBalance(ctx, accountID, assetID)))
}

/**
 * Query that return the historic events a given account have been involved in
 *
 * @param config configuration for the FT module
 * @param ctx contextual information
 * @param args arguments including account identifier and asset identifier
 * @return history entries relating to the given account and asset
 */
fun ftHistoryQ(config: FTConfig, ctx: EContext, args: Gtv): Gtv {
    val accountID = args["account_id"]!!.asByteArray(true)
    val assetID = args["asset_id"]!!.asString()
    val history = config.dbOps.getHistory(ctx, accountID, assetID)
    val entries = history.map({
        gtv(
                "delta" to gtv(it.delta),
                "tx_rid" to gtv(it.txRID),
                "op_index" to gtv(it.opIndex.toLong()),
                "memo" to (if (it.memo != null) gtv(it.memo) else GtvNull)
        )
    }).toTypedArray()
    return gtv(*entries)
}
