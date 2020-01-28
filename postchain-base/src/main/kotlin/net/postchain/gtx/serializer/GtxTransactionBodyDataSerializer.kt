// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx.serializer

import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXTransactionBodyData

object GtxTransactionBodyDataSerializer {

    /**
     * Elements should be ordered like this:
     * 1. blockchainRId [GtvByteArray]
     * 2. operations [GtvArray]
     * 3. signers [GtvArray]
     */
    fun serializeToGtv(txBody: GTXTransactionBodyData): GtvArray {

        //  1. blockchainRId
        val blockchainRid: GtvByteArray = gtv(txBody.blockchainRID)

        // 2. operations
        val opsGtvArr: GtvArray = gtv(txBody.operations.map{OpDataSerializer.serializeToGtv(it)})

        // 3. signers
        val signersGtvArr: GtvArray = gtv(txBody.signers.map{ gtv(it)})

        return gtv(blockchainRid, opsGtvArr, signersGtvArr)
    }
}