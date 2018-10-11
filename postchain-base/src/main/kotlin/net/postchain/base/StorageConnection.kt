package net.postchain.base

import net.postchain.core.EContext

fun <RT> withReadConnection(storage: Storage, chainID: Long, op: (EContext) -> RT): RT {
    val ctx = storage.openReadConnection(chainID)
    try {
        return op(ctx)
    } finally {
        storage.closeReadConnection(ctx)
    }
}

fun withWriteConnection(storage: Storage, chainID: Long, op: (EContext) -> Boolean): Boolean {
    val ctx = storage.openWriteConnection(chainID)
    var commit = false

    try {
        commit = op(ctx)
    } finally {
        storage.closeWriteConnection(ctx, commit)
    }

    return commit
}
