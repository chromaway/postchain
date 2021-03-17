// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx.factory

import net.postchain.base.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvString
import net.postchain.gtx.GTXTransactionBodyData
import net.postchain.gtx.GTXTransactionData
import net.postchain.gtx.OpData
import org.junit.Assert
import org.junit.Test

class GtxDataFactoryTest {

    val op_name = "op_name"
    val op_args = listOf(1,2,3,4)
    val blockchainRID = BlockchainRid.ZERO_RID
    val aliceSigner: ByteArray = "010101".hexStringToByteArray()
    val aliceSignature: ByteArray = "DDDDDD".hexStringToByteArray()

    @Test
    fun testBuildGTXDataFromGtv_one_operation() {

        // ---------- Build expected  ----------------------
        val expectedOp = OpData(op_name, op_args.map{ gtv(it.toLong())}.toTypedArray() )
        val expectedTxBody = GTXTransactionBodyData(blockchainRID, arrayOf(expectedOp), arrayOf(aliceSigner))
        val expectedTx = GTXTransactionData(expectedTxBody, arrayOf(aliceSignature))

        // ---------- Build da GTV struct -----------------

        // Operation Data
        val opName: GtvString = gtv(op_name)
        val opArgs: GtvArray = gtv(op_args.map{ gtv(it.toLong())} )
        val op1: GtvArray = gtv(listOf(opName, opArgs))

        // Transaction Body Data
        val bcRid: GtvByteArray = gtv(blockchainRID)
        val operations: GtvArray = gtv(listOf(op1))
        val signers: GtvArray = gtv(listOf(gtv(aliceSigner)))
        val txb: GtvArray = gtv(listOf(bcRid, operations, signers))

        // Transaction Data
        val signatures: GtvArray = gtv(listOf(gtv(aliceSignature)))
        val tx = gtv(listOf(txb, signatures))

        // ---------- Convert it ------------------------------
        val txData: GTXTransactionData = GtxTransactionDataFactory.deserializeFromGtv(tx)

        Assert.assertEquals(txData, expectedTx)
    }
}