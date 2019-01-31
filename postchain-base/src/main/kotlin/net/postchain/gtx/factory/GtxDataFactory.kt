package net.postchain.gtx.factory

import net.postchain.gtv.Gtv
import net.postchain.gtx.*

object GtxDataFactory {

    /**
     * Takes a [Gtv] and transforms it to [GTXData] or throws [IllegalArgumentException]
     */
    fun deserializeFromGtv(gtv: Gtv): GTXData {

        val gtxTD: GTXTransactionData = GtxTransactionDataFactory.deserializeFromGtv(gtv)
        val gtxTBD = gtxTD.transactionBodyData

        return GTXData(gtxTBD.blockchainRID, gtxTBD.signers, gtxTD.signatures, gtxTBD.operations)
    }
}
