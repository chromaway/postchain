// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.modules.ft

import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.core.Transaction
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.PostchainTestNode
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXDataBuilder
import net.postchain.modules.ft.AccountUtil
import net.postchain.modules.ft.BasicAccount

//val testBlockchainRID = BlockchainRid.buildFromHex("78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3")
val myCS = SECP256K1CryptoSystem()

open class FTIntegrationTest : IntegrationTestSetup() {

    val issuerPubKeys = arrayOf(pubKey(0), pubKey(1))
    val issuerPrivKeys = arrayOf(privKey(0), privKey(1))
    val aliceIdx = 1
    val bobIdx = 2
    val alicePubKey = pubKey(aliceIdx)
    val alicePrivKey = privKey(aliceIdx)
    val bobPubKey = pubKey(bobIdx)
    val bobPrivKey = privKey(bobIdx)

    lateinit var accUtil: AccountUtil
    lateinit var issuerID: ByteArray
    lateinit var aliceAccountDesc: ByteArray
    lateinit var aliceAccountID: ByteArray
    lateinit var bobAccountDesc: ByteArray
    lateinit var bobAccountID: ByteArray
    lateinit var invalidAccountID: ByteArray

    lateinit var testBlockchainRID: BlockchainRid

    fun setBlockchainRid(bcRid: BlockchainRid) {
        testBlockchainRID = bcRid
        accUtil = AccountUtil(bcRid, SECP256K1CryptoSystem())
        issuerID = accUtil.makeAccountID(accUtil.issuerAccountDesc(issuerPubKeys[0]))
        aliceAccountDesc = BasicAccount.makeDescriptor(bcRid.data, alicePubKey)
        aliceAccountID = accUtil.makeAccountID(aliceAccountDesc)
        bobAccountDesc = BasicAccount.makeDescriptor(bcRid.data, bobPubKey)
        bobAccountID = accUtil.makeAccountID(bobAccountDesc)
        invalidAccountID = accUtil.makeAccountID("hello".toByteArray())
    }

    fun makeRegisterTx(accountDescs: Array<ByteArray>, registrator: Int): ByteArray {
        val b = GTXDataBuilder(testBlockchainRID!!, arrayOf(pubKey(registrator)), myCS)
        for (desc in accountDescs) {
            b.addOperation("ft_register", arrayOf(gtv(desc)))
        }
        b.finish()
        b.sign(myCS.buildSigMaker(pubKey(registrator), privKey(registrator)))
        return b.serialize()
    }

    fun makeIssueTx(issuerIdx: Int, issuerID: ByteArray, recipientID: ByteArray, assetID: String, amout: Long): ByteArray {
        val b = GTXDataBuilder(testBlockchainRID!!, arrayOf(issuerPubKeys[issuerIdx]), myCS)
        b.addOperation("ft_issue", arrayOf(
                gtv(issuerID), gtv(assetID), gtv(amout), gtv(recipientID)
        ))
        b.finish()
        b.sign(myCS.buildSigMaker(issuerPubKeys[issuerIdx], issuerPrivKeys[issuerIdx]))
        return b.serialize()
    }

    fun makeTransferTx(senderIdx: Int,
                       senderID: ByteArray,
                       assetID: String,
                       amout: Long,
                       recipientID: ByteArray,
                       memo1: String? = null, memo2: String? = null): ByteArray {
        return makeTransferTx(pubKey(senderIdx), privKey(senderIdx), senderID, assetID, amout, recipientID, memo1, memo2)
    }

    fun makeTransferTx(senderPubKey: ByteArray,
                       senderPrivKey: ByteArray,
                       senderID: ByteArray,
                       assetID: String,
                       amout: Long,
                       recipientID: ByteArray,
                       memo1: String? = null, memo2: String? = null): ByteArray {
        val b = GTXDataBuilder(testBlockchainRID!!, arrayOf(senderPubKey), myCS)

        val args = mutableListOf<Gtv>()
        args.add(gtv(gtv(gtv(senderID), gtv(assetID), gtv(amout)))) // inputs

        val output = mutableListOf<Gtv>(gtv(recipientID), gtv(assetID), gtv(amout))
        if (memo2 != null) {
            output.add(gtv("memo" to gtv(memo2)))
        }
        args.add(gtv(gtv(*output.toTypedArray()))) // outputs
        if (memo1 != null) {
            args.add(gtv("memo" to gtv(memo1)))
        }
        b.addOperation("ft_transfer", args.toTypedArray())
        b.finish()
        b.sign(myCS.buildSigMaker(senderPubKey, senderPrivKey))
        return b.serialize()
    }

    fun enqueueTx(node: PostchainTestNode, data: ByteArray): Transaction? {
        return enqueueTx(node, data, -1)
    }

}