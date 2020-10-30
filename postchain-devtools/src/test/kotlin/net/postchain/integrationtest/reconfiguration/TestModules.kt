// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.integrationtest.reconfiguration

import net.postchain.core.EContext
import net.postchain.core.Transactor
import net.postchain.core.TxEContext
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtx.ExtOpData
import net.postchain.gtx.GTXModule

open class AbstractDummyModule : GTXModule {

    override fun makeTransactor(opData: ExtOpData): Transactor {
        return object : Transactor {
            override fun isSpecial(): Boolean {
                return false
            }

            override fun isCorrect(): Boolean = true
            override fun apply(ctx: TxEContext): Boolean = true
        }
    }

    override fun getOperations(): Set<String> = emptySet()

    override fun getQueries(): Set<String> = emptySet()

    override fun query(ctxt: EContext, name: String, args: Gtv): Gtv = GtvNull

    override fun initializeDB(ctx: EContext) = Unit
}

class DummyModule1 : AbstractDummyModule()

class DummyModule2 : AbstractDummyModule()

class DummyModule3 : AbstractDummyModule()

class DummyModule4 : AbstractDummyModule()
