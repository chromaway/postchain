// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.core

interface Network {
    fun isPrimary(): Boolean
    fun broadcastTx(txData: ByteArray)
}