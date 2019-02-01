package net.postchain.gtx.factory

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvString
import net.postchain.gtx.GTXTransactionBodyData
import net.postchain.gtx.GtxBase
import net.postchain.gtx.OpData


object GtxTransactionBodyDataFactory {

    /**
     * Elements should be ordered like this:
     * 1. blockchainRId [GtvByteArray]
     * 2. operations [GtvArray]
     * 3. signers [GtvArray]
     */
    fun deserializeFromGtv(gtv: Gtv): GTXTransactionBodyData {
        // Check base structure
        val mainArr = gtv as GtvArray
        FactoryUtils.formatChecker(mainArr, GtxBase.NR_FIELDS_TRANSACTION_BODY, "GTXTransactionBodyData")

        //  1. blockchainRId
        val blockchainRid: ByteArray = (mainArr[0] as GtvByteArray).bytearray

        // 2. operations
        val opsGtvArr = (mainArr[1] as GtvArray)
        val ops: Array<OpData> = opsGtvArr.array.map { OpDataFactory.deserializeFromGtv(it) }.toTypedArray()

        // 3. signers
        val signersGtvArr = (mainArr[2] as GtvArray)
        val signers: Array<ByteArray> = signersGtvArr.array.map { (it as GtvByteArray).bytearray }.toTypedArray()

        return GTXTransactionBodyData(blockchainRid, ops, signers)
    }
}