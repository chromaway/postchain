// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.modules.esplix

import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.toHex
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXDataBuilder
import net.postchain.modules.esplix_r4.computeChainID
import net.postchain.modules.esplix_r4.computeMessageID
import org.junit.Assert
import org.junit.Test

class EsplixIntegrationTest : IntegrationTestSetup() {

    val myCS = SECP256K1CryptoSystem()

    private fun makeCreateChainTx(creator: Int, nonce: ByteArray, payload: ByteArray, bcRid: BlockchainRid): ByteArray {
        val b = GTXDataBuilder(bcRid, arrayOf(pubKey(creator)), myCS)
        b.addOperation("R4createChain", arrayOf(
                gtv(nonce),
                gtv(payload)))
        b.finish()
        b.sign(myCS.buildSigMaker(pubKey(creator), privKey(creator)))
        return b.serialize()

    }

    private fun makePostMessage(poster: Int, prevID: ByteArray, payload: ByteArray, bcRid: BlockchainRid): ByteArray {
        val b = GTXDataBuilder(bcRid, arrayOf(pubKey(poster)), myCS)
        b.addOperation("R4postMessage", arrayOf(
                gtv(prevID),
                gtv(payload)))
        b.finish()
        b.sign(myCS.buildSigMaker(pubKey(poster), privKey(poster)))
        return b.serialize()
    }

    @Test
    fun testEsplix() {
        val creator = 0
        val user = 1
        val nodes = createNodes(1, "/net/postchain/esplix/blockchain_config.xml")
        val node = nodes[0]
        var currentBlockHeight = -1L

        fun buildBlockAndCommitWithTx(data: ByteArray, fail: Boolean = false) {
            currentBlockHeight += 1
            try {
                val tx = node.getBlockchainInstance().getEngine().getConfiguration().getTransactionFactory().decodeTransaction(data)
                node.getBlockchainInstance().getEngine().getTransactionQueue().enqueue(tx)
                buildBlockAndCommit(node.getBlockchainInstance().getEngine())
                Assert.assertEquals(currentBlockHeight, getBestHeight(node))
                val txSz = getTxRidsAtHeight(node, currentBlockHeight).size
                if (fail)
                    Assert.assertEquals(0, txSz)
                else
                    Assert.assertEquals(1, txSz)
            } catch (e: Error) {
                println(e)
            }
        }

        val payload = ByteArray(50) { 1 }
        val nonce = cryptoSystem.getRandomBytes(32)
        val bcRid = node.getBlockchainRid(1L)!!

        val createChainTx = makeCreateChainTx(
                creator,
                nonce,
                payload,
                bcRid)
        buildBlockAndCommitWithTx(createChainTx)

        val chainID = computeChainID(cryptoSystem,
                bcRid.data, nonce, payload,
                arrayOf(pubKey(creator))
        )
        val postmessageTx = makePostMessage(
                creator,
                chainID,
                payload,
                bcRid)
        buildBlockAndCommitWithTx(postmessageTx)

        val messageID = computeMessageID(cryptoSystem, chainID, payload, arrayOf(pubKey(creator)))
        val postMessageTx2 = makePostMessage(
                user,
                messageID,
                payload,
                bcRid)
        buildBlockAndCommitWithTx(postMessageTx2)

        val messageID2 = computeMessageID(cryptoSystem, messageID, payload, arrayOf(pubKey(user)))
        val postMessageTx3 = makePostMessage(
                creator,
                messageID2,
                payload,
                bcRid)
        buildBlockAndCommitWithTx(postMessageTx3)

        //Deliberately try to post a message that has the incorrect prevID
        val postMessageTx4 = makePostMessage(
                creator,
                ByteArray(32) { 0 },
                payload,
                bcRid)
        buildBlockAndCommitWithTx(postMessageTx4, true)

        val msg1 = node.getBlockchainInstance().getEngine().getBlockQueries().query(
                """{"type":"R4getMessages",
                    "chainID":"${chainID.toHex()}",
                    "maxHits":1
                   }""")
        println(msg1.get())
        val msg2 = node.getBlockchainInstance().getEngine().getBlockQueries().query(
                """{"type":"R4getMessages",
                    "chainID":"${chainID.toHex()}",
                    "sinceMessageID":"${messageID.toHex()}",
                    "maxHits":1
                   }""")
        println(msg2.get())

    }
}