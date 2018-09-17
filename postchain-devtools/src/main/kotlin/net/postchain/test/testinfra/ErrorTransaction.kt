package net.postchain.test.testinfra

import net.postchain.core.TxEContext
import net.postchain.core.UserMistake

class ErrorTransaction(id: Int, private val applyThrows: Boolean, private val isCorrectThrows: Boolean) : TestTransaction(id) {
    override fun isCorrect(): Boolean {
        if (isCorrectThrows) throw UserMistake("Thrown from isCorrect()")
        return true
    }

    override fun apply(ctx: TxEContext): Boolean {
        if (applyThrows) throw UserMistake("Thrown from apply()")
        return true
    }
}