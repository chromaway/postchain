package net.postchain.gtx.factory

import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvByteArray
import org.junit.Assert
import org.junit.Test
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvString
import net.postchain.gtx.GTXData
import net.postchain.gtx.GtxHelper.convertToByteArray
import net.postchain.gtx.OpData

class GtxDataFactoryTest {

    val op_name = "op_name"
    val op_args = listOf(1,2,3,4)
    val blockchainRID: ByteArray = convertToByteArray("222222")
    val aliceSigner: ByteArray = convertToByteArray("010101")
    val aliceSignature: ByteArray = convertToByteArray("DDDDDD")

    @Test
    fun testBuildGTXDataFromGtv_one_operation() {

        // ---------- Build expected  ----------------------
        val expectedOp = OpData(op_name, op_args.map{ gtv(it.toLong())}.toTypedArray() )
        val expectedData = GTXData(blockchainRID, arrayOf(aliceSigner), arrayOf(aliceSignature), arrayOf(expectedOp))

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
        val data: GTXData = GtxDataFactory.deserializeFromGtv(tx)

        Assert.assertEquals(data, expectedData)
    }
}