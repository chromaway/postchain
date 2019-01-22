package net.postchain.devtools.testinfra

import net.postchain.core.TxEContext

class UnexpectedExceptionTransaction(id: Int) : TestTransaction(id) {
    override fun apply(ctx: TxEContext): Boolean {
        throw RuntimeException("Expected exception")
    }
}