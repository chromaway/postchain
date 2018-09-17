package net.postchain.test.modules.esplix

import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.gtx.GTXDataBuilder
import net.postchain.gtx.gtx
import net.postchain.modules.esplix_r4.BaseEsplixModuleFactory
import net.postchain.modules.esplix_r4.computeChainID
import net.postchain.modules.esplix_r4.computeMessageID
import net.postchain.test.IntegrationTest
import net.postchain.test.KeyPairHelper.Companion.privKey
import net.postchain.test.KeyPairHelper.Companion.pubKey
import org.junit.Assert
import org.junit.Test

val testBlockchainRID = "78967baa4768cbcef11c508326ffb13a956689fcb6dc3ba17f4b895cbb1577a3".hexStringToByteArray()
val myCS = SECP256K1CryptoSystem()

class EsplixIntegrationTest : IntegrationTest() {

    fun makeCreateChainTx(creator: Int, nonce: ByteArray, payload: ByteArray): ByteArray {
        val b = GTXDataBuilder(testBlockchainRID, arrayOf(pubKey(creator)), myCS)
        b.addOperation("R4createChain", arrayOf(
                gtx(nonce),
                gtx(payload)))
        b.finish()
        b.sign(myCS.makeSigner(pubKey(creator), privKey(creator)))
        return b.serialize()

    }

    fun makePostMessage(poster: Int, prevID: ByteArray, payload: ByteArray): ByteArray {
        val b = GTXDataBuilder(testBlockchainRID, arrayOf(pubKey(poster)), myCS)
        b.addOperation("R4postMessage", arrayOf(
                gtx(prevID),
                gtx(payload)))
        b.finish()
        b.sign(myCS.makeSigner(pubKey(poster), privKey(poster)))
        return b.serialize()
    }

    @Test
    fun testEsplix() {
        configOverrides.setProperty("blockchain.1.configurationfactory",
                GTXBlockchainConfigurationFactory::class.qualifiedName)
        configOverrides.setProperty("blockchain.1.gtx.modules",
                BaseEsplixModuleFactory::class.qualifiedName)

        val creator = 0
        val user = 1
        val (node, chainId) = createNode(0)
        var currentBlockHeight = -1L

        fun buildBlockAndCommitWithTx(data: ByteArray, fail: Boolean = false) {
            currentBlockHeight += 1
            try {
                val tx = node.getBlockchainInstance(chainId).blockchainConfiguration.getTransactionFactory().decodeTransaction(data)
                node.getBlockchainInstance(chainId).getEngine().getTransactionQueue().enqueue(tx)
                buildBlockAndCommit(node.getBlockchainInstance(chainId).getEngine())
                Assert.assertEquals(currentBlockHeight, getBestHeight(node, chainId))
                val txSz = getTxRidsAtHeight(node, chainId, currentBlockHeight).size
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

        val createChainTx = makeCreateChainTx(
                creator,
                nonce,
                payload)
        buildBlockAndCommitWithTx(createChainTx)

        val chainID = computeChainID(cryptoSystem,
                testBlockchainRID, nonce, payload,
                arrayOf(pubKey(creator))
        )
        val postmessageTx = makePostMessage(
                creator,
                chainID,
                payload)
        buildBlockAndCommitWithTx(postmessageTx)

        val messageID = computeMessageID(cryptoSystem, chainID, payload, arrayOf(pubKey(creator)))
        val postMessageTx2 = makePostMessage(
                user,
                messageID,
                payload
        )
        buildBlockAndCommitWithTx(postMessageTx2)

        val messageID2 = computeMessageID(cryptoSystem, messageID, payload, arrayOf(pubKey(user)))
        val postMessageTx3 = makePostMessage(
                creator,
                messageID2,
                payload
        )
        buildBlockAndCommitWithTx(postMessageTx3)

        //Deliberately try to post a message that has the incorrect prevID
        val postMessageTx4 = makePostMessage(
                creator,
                ByteArray(32) { 0 },
                payload
        )
        buildBlockAndCommitWithTx(postMessageTx4, true)

        val msg1 = node.getBlockchainInstance(chainId).getEngine().getBlockQueries().query(
                """{"type":"R4getMessages",
                    "chainID":"${chainID.toHex()}",
                    "maxHits":1
                   }""")
        println(msg1.get())
        val msg2 = node.getBlockchainInstance(chainId).getEngine().getBlockQueries().query(
                """{"type":"R4getMessages",
                    "chainID":"${chainID.toHex()}",
                    "sinceMessageID":"${messageID.toHex()}",
                    "maxHits":1
                   }""")
        println(msg2.get())

    }
}