package net.postchain.gtx

import net.postchain.core.UserMistake
import net.postchain.devtools.IntegrationTest
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SQLModuleIntegrationTest : IntegrationTest() {

    private fun makeTx(ownerIdx: Int, key: String, value: String): ByteArray {
        val owner = pubKey(ownerIdx)
        return GTXDataBuilder(net.postchain.devtools.gtx.testBlockchainRID, arrayOf(owner), net.postchain.devtools.gtx.myCS).run {
            addOperation("test_set_value", arrayOf(gtx(key), gtx(value), gtx(owner)))
            finish()
            sign(net.postchain.devtools.gtx.myCS.makeSigner(owner, privKey(ownerIdx)))
            serialize()
        }
    }

    @Test
    fun testBuildBlock() {
        val sqlModulePath = Paths.get(javaClass.getResource("sqlmodule1.sql").toURI()).toString()
        /* FYI: [et]: Commons config has been used again instead of gtx-config
        gtxConfig = gtx(
                "configurationfactory" to gtx(GTXBlockchainConfigurationFactory::class.qualifiedName!!),
                "signers" to gtxConfigSigners(),
                "gtx" to gtx(
                        "modules" to gtx(listOf(gtx(SQLGTXModuleFactory::class.qualifiedName!!))),
                        "sqlmodules" to gtx(listOf(gtx(sqlModulePath)))
                )
        )
        */

        configOverrides.setProperty("blockchain.1.configurationfactory", GTXBlockchainConfigurationFactory::class.qualifiedName)
        configOverrides.setProperty("blockchain.1.gtx.modules", listOf(SQLGTXModuleFactory::class.qualifiedName))
        configOverrides.setProperty("blockchain.1.gtx.sqlmodules", listOf(sqlModulePath))

//        val node = createDataLayerNG(0)
        val node = createNode(0)

        enqueueTx(node, makeTx(0, "k", "v"), 0)
        buildBlockAndCommit(node)
        enqueueTx(node, makeTx(0, "k", "v2"), 1)
        enqueueTx(node, makeTx(0, "k2", "v2"), 1)
        enqueueTx(node, makeTx(1, "k", "v"), -1)
        buildBlockAndCommit(node)

        verifyBlockchainTransactions(node)

        val blockQueries = node.getBlockchainInstance().getEngine().getBlockQueries()
        assertFailsWith<UserMistake> {
            blockQueries.query("""{tdype: 'test_get_value'}""").get()
        }

        assertFailsWith<UserMistake> {
            blockQueries.query("""{type: 'non-existing'}""").get()
        }

        val gson = make_gtx_gson()

        var result = blockQueries.query("""{type: 'test_get_value', q_key: 'hello'}""").get()
        var gtxResult = gson.fromJson<GTXValue>(result, GTXValue::class.java)
        assertEquals(0, gtxResult.getSize())

        result = blockQueries.query("""{type: 'test_get_value', q_key: 'k'}""").get()
        gtxResult = gson.fromJson<GTXValue>(result, GTXValue::class.java)
        assertEquals(1, gtxResult.getSize())
        val hit0 = gtxResult[0].asDict()
        assertNotNull(hit0["val"])
        assertEquals("v2", hit0["val"]!!.asString())
        assertNotNull(hit0["owner"])
        assertTrue(pubKey(0).contentEquals(hit0["owner"]!!.asByteArray(true)))

        result = blockQueries.query("""{type: 'test_get_count'}""").get()
        gtxResult = gson.fromJson<GTXValue>(result, GTXValue::class.java)
        assertEquals(1, gtxResult.getSize())
        assertEquals(1, gtxResult[0]["nbigint"]!!.asInteger())
        assertEquals(2, gtxResult[0]["ncount"]!!.asInteger())

        println(result)
    }

}