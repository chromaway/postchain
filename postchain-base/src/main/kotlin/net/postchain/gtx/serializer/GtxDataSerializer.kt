package net.postchain.gtx.serializer

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtx.GTXData
import net.postchain.gtx.GTXTransactionBodyData
import net.postchain.gtx.GTXTransactionData

object GtxDataSerializer {

    /**
     * Takes a  [GTXData] and transforms it to [Gtv] or throws [IllegalArgumentException]
     */
    fun serializeToGtv(gtxData: GTXData): GtvArray {

        // 1. Transform the GTXData to our structure
        val txb = GTXTransactionBodyData(gtxData.blockchainRID, gtxData.operations, gtxData.signers)
        val tx = GTXTransactionData(txb, gtxData.signatures)

        // 2. Transform it to GTV
        return GtxTransactionDataSerializer.serializeToGtv(tx)
    }
}