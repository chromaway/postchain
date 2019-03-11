package net.postchain.integrationtest.configurations

import net.postchain.core.EContext
import net.postchain.core.Transactor
import net.postchain.core.TxEContext
import net.postchain.gtx.ExtOpData
import net.postchain.gtx.GTXModule
import net.postchain.gtx.GTXNull
import net.postchain.gtx.GTXValue

open class AbstractDummyModule : GTXModule {

    override fun makeTransactor(opData: ExtOpData): Transactor {
        return object : Transactor {
            override fun isCorrect(): Boolean = true
            override fun apply(ctx: TxEContext): Boolean = true
        }
    }

    override fun getOperations(): Set<String> = emptySet()

    override fun getQueries(): Set<String> = emptySet()

    override fun query(ctxt: EContext, name: String, args: GTXValue): GTXValue = GTXNull

    override fun initializeDB(ctx: EContext) = Unit
}

class DummyModule1 : AbstractDummyModule()

class DummyModule2 : AbstractDummyModule()
