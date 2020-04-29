// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx.factory

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtx.GTXTransactionBodyData
import net.postchain.gtx.GTXTransactionData
import net.postchain.gtx.GtxBase

object GtxTransactionDataFactory {

    /**
     * Elements should be ordered like this:
     * 1. transaction data body [GtvByteArray]
     * 2. signatures [GtvArray]
     */
    fun deserializeFromGtv(gtv: Gtv): GTXTransactionData {
        // Check base structure
        val mainArr = gtv as GtvArray
        FactoryUtils.formatChecker(mainArr, GtxBase.NR_FIELDS_TRANSACTION, "GTXTransactionData")

        // 1. transaction data body
        val opsGtvArr = (mainArr[0] as GtvArray)
        val transactionBody: GTXTransactionBodyData = GtxTransactionBodyDataFactory.deserializeFromGtv(opsGtvArr)

        // 2. signatures
        val signaturesGtvArr = (mainArr[1] as GtvArray)
        val signatures: Array<ByteArray> = signaturesGtvArr.array.map { (it as GtvByteArray).bytearray }.toTypedArray()

        return GTXTransactionData(transactionBody, signatures)
    }
}