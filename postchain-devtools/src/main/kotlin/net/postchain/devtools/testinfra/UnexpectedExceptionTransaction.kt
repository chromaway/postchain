// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.testinfra

import net.postchain.core.TxEContext

class UnexpectedExceptionTransaction(id: Int) : TestTransaction(id) {
    override fun apply(ctx: TxEContext): Boolean {
        throw RuntimeException("Expected exception")
    }
}