// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx

import net.postchain.core.EContext
import net.postchain.core.TxEContext

/**
 * nop operation that replaces GtxNop and doesn't impose any size or argument restrictions.
 * This module was created to cope with incompatibilities in old blockchains that had
 * erroneous nops in them.
 */
class GtxPermissiveNop(u: Unit, opData: ExtOpData) : GTXOperation(opData) {
    override fun apply(ctx: TxEContext): Boolean = true

    override fun isCorrect(): Boolean = true
}

class PatchOpsGTXModule : SimpleGTXModule<Unit>(Unit, mapOf("nop" to ::GtxPermissiveNop), mapOf()) {
    override fun initializeDB(ctx: EContext) {}
}