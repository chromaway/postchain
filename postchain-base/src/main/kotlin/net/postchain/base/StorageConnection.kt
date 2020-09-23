// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import net.postchain.StorageBuilder
import net.postchain.config.app.AppConfig
import net.postchain.core.AppContext
import net.postchain.core.EContext
import net.postchain.core.NODE_ID_NA

fun <RT> Storage.withReadConnection(op: (AppContext) -> RT): RT {
    val ctx = openReadConnection()
    try {
        return op(ctx)
    } finally {
        closeReadConnection(ctx)
    }
}

/*
fun Storage.withWriteConnection(op: (AppContext) -> Boolean): Boolean {
    val ctx = openWriteConnection()
    var commit = false

    try {
        commit = op(ctx)
    } finally {
        closeWriteConnection(ctx, commit)
    }

    return commit
}
 */

fun <RT> Storage.withWriteConnection(op: (AppContext) -> RT): RT {
    val ctx = openWriteConnection()
    var commit = false

    return try {
        val res = op(ctx)
        commit = true
        res
    } finally {
        closeWriteConnection(ctx, commit)
    }
}


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

    return try {
        val ret = op(ctx)
        commit = true
        ret
    } finally {
        storage.closeWriteConnection(ctx, commit)
    }
}

fun <RT> runStorageCommand(appConfig: AppConfig, op: (ctx: AppContext) -> RT): RT {
    val storage = StorageBuilder.buildStorage(appConfig, NODE_ID_NA)

    return storage.use {
        it.withWriteConnection { ctx ->
            op(ctx)
        }
    }
}

fun <RT> runStorageCommand(nodeConfigFile: String, op: (ctx: AppContext) -> RT): RT {
    val appConfig = AppConfig.fromPropertiesFile(nodeConfigFile)
    return runStorageCommand(appConfig, op)
}

fun <RT> runStorageCommand(appConfig: AppConfig, chainId: Long, op: (ctx: EContext) -> RT): RT {
    val storage = StorageBuilder.buildStorage(appConfig, NODE_ID_NA)

    return storage.use {
        withReadWriteConnection(it, chainId) { ctx ->
            op(ctx)
        }
    }
}

fun <RT> runStorageCommand(nodeConfigFile: String, chainId: Long, op: (ctx: EContext) -> RT): RT {
    val appConfig = AppConfig.fromPropertiesFile(nodeConfigFile)
    return runStorageCommand(appConfig, chainId, op)
}