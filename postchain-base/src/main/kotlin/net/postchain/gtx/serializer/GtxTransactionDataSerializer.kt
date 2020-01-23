// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx.serializer

import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXTransactionData

object GtxTransactionDataSerializer {

    /**
     * Elements should be ordered like this:
     * 1. transaction data body [GtvByteArray]
     * 2. signatures [GtvArray]
     */
    fun serializeToGtv(tx: GTXTransactionData): GtvArray {

        // 1. transaction data body
        val txBodyGtvArr: GtvArray = GtxTransactionBodyDataSerializer.serializeToGtv(tx.transactionBodyData)

        // 2. signatures
        val signaturesGtvArr: GtvArray = gtv(tx.signatures.map { gtv(it)})

        return gtv(txBodyGtvArr, signaturesGtvArr)
    }
}