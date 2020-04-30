// Copyright (c) 2020 ChromaWay AB. See README for license information.

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

/**
 * Use this when your writing operation has a return type
 *
 * @param storage is the storage
 * @param chainID is the chain we work on
 * @param op is an operation with return type RT (parametrict type)
 * @return the same object as "op"
 */
fun <RT> withReadWriteConnection(storage: Storage, chainID: Long, op: (EContext) -> RT): RT {
    val ctx = storage.openWriteConnection(chainID)
    var commit = false
    try {
        val ret = op(ctx)
        commit = true
        return ret
    } finally {
        storage.closeWriteConnection(ctx, commit)
    }

}
