package net.postchain.e2e.toolstests

import assertk.assert
import assertk.assertions.isEqualTo
import net.postchain.e2e.tools.parseLogLastHeight
import net.postchain.e2e.tools.postgresUrl
import org.junit.Test
import kotlin.test.assertEquals


class ToolsTest {

    @Test
    fun testPostgresUrl() {
        val expected = "jdbc:postgresql://my-host:7777/postchain"
        val actual = postgresUrl("my-host", 7777)

        assertEquals(expected, actual)
    }

    @Test
    fun testParseLogHeight_0() {
        val log = """
            INFO  2019-11-20 14:00:34.175 [-1-BaseBlockDatabaseWorker] BaseBlockchainEngine - 
            [02:0C]/[D9:26] Loaded block: 42 ms, 0 net tps, 0 gross tps, height: 0, accepted txs: 0, rejected txs: 0, 
            root-hash: 46AF9064F12528CAD6A7C377204ACD0AC38CDC6912903E7DAB3703764C8DD5E5, 
            block-rid: 13779FAED9F36CB2411949E88D9D953FEC574ACB5676ABC4B9A69968A8FE8451, 
            prev-block-rid: 0000000000000000000000000000000000000000000000000000000000000000
        """.trimIndent()

        val expected = 0
        val actual = parseLogLastHeight(log)

        assertEquals(expected, actual)
    }

    @Test
    fun testParseLogHeight_Positive() {
        val log = """
            INFO  2019-11-20 14:00:34.175 [-1-BaseBlockDatabaseWorker] BaseBlockchainEngine - 
            [02:0C]/[D9:26] Loaded block: 42 ms, 0 net tps, 0 gross tps, height: 7, accepted txs: 0, rejected txs: 0, 
            root-hash: 46AF9064F12528CAD6A7C377204ACD0AC38CDC6912903E7DAB3703764C8DD5E5, 
            block-rid: 13779FAED9F36CB2411949E88D9D953FEC574ACB5676ABC4B9A69968A8FE8451, 
            prev-block-rid: 0000000000000000000000000000000000000000000000000000000000000000
        """.trimIndent()

        val expected = 7
        val actual = parseLogLastHeight(log)

        assertEquals(expected, actual)
    }

    @Test
    fun testParseLogHeight_noHeightGiven_returnsNull() {
        val log = """log message w/o height""".trimIndent()

        val expected = null
        val actual = parseLogLastHeight(log)

        assertEquals(expected, actual)
    }

    @Test
    fun testParseLogHeight_LastHeight() {
        val log = """
            INFO  2019-11-20 14:00:34.175 [-1-BaseBlockDatabaseWorker] BaseBlockchainEngine - 
            [02:0C]/[D9:26] Loaded block: 42 ms, 0 net tps, 0 gross tps, height: 7, accepted txs: 0, rejected txs: 0, 
            root-hash: 46AF9064F12528CAD6A7C377204ACD0AC38CDC6912903E7DAB3703764C8DD5E5, 
            block-rid: 13779FAED9F36CB2411949E88D9D953FEC574ACB5676ABC4B9A69968A8FE8451, 
            prev-block-rid: 0000000000000000000000000000000000000000000000000000000000000000
            
            INFO  2019-11-20 14:00:34.175 [-1-BaseBlockDatabaseWorker] BaseBlockchainEngine - 
            [02:0C]/[D9:26] Loaded block: 42 ms, 0 net tps, 0 gross tps, height: 8, accepted txs: 0, rejected txs: 0, 
            root-hash: 46AF9064F12528CAD6A7C377204ACD0AC38CDC6912903E7DAB3703764C8DD5E5, 
            block-rid: 13779FAED9F36CB2411949E88D9D953FEC574ACB5676ABC4B9A69968A8FE8451, 
            prev-block-rid: 0000000000000000000000000000000000000000000000000000000000000000
            
            INFO  2019-11-20 14:00:34.175 [-1-BaseBlockDatabaseWorker] BaseBlockchainEngine - 
            [02:0C]/[D9:26] Loaded block: 42 ms, 0 net tps, 0 gross tps, height: 9, accepted txs: 0, rejected txs: 0, 
            root-hash: 46AF9064F12528CAD6A7C377204ACD0AC38CDC6912903E7DAB3703764C8DD5E5, 
            block-rid: 13779FAED9F36CB2411949E88D9D953FEC574ACB5676ABC4B9A69968A8FE8451, 
            prev-block-rid: 0000000000000000000000000000000000000000000000000000000000000000
        """.trimIndent()

        val expected = 9
        val actual = parseLogLastHeight(log)

        assertEquals(expected, actual)
    }

    @Test
    fun testParseLogHeight_TwoChains() {
        val log = """
            INFO  2019-11-20 14:00:34.175 [-1-BaseBlockDatabaseWorker] BaseBlockchainEngine - 
            [02:0C]/[D9:26] Loaded block: 42 ms, 0 net tps, 0 gross tps, height: 7, accepted txs: 0, rejected txs: 0, 
            root-hash: 46AF9064F12528CAD6A7C377204ACD0AC38CDC6912903E7DAB3703764C8DD5E5, 
            block-rid: 13779FAED9F36CB2411949E88D9D953FEC574ACB5676ABC4B9A69968A8FE8451, 
            prev-block-rid: 0000000000000000000000000000000000000000000000000000000000000000
            
            INFO  2019-11-20 14:00:34.175 [-1-BaseBlockDatabaseWorker] BaseBlockchainEngine - 
            [02:0C]/[D9:26] Loaded block: 42 ms, 0 net tps, 0 gross tps, height: 8, accepted txs: 0, rejected txs: 0, 
            root-hash: 46AF9064F12528CAD6A7C377204ACD0AC38CDC6912903E7DAB3703764C8DD5E5, 
            block-rid: 13779FAED9F36CB2411949E88D9D953FEC574ACB5676ABC4B9A69968A8FE8451, 
            prev-block-rid: 0000000000000000000000000000000000000000000000000000000000000000
            
            INFO  2019-11-20 14:00:34.175 [-1-BaseBlockDatabaseWorker] BaseBlockchainEngine - 
            [02:0C]/[D9:99] Loaded block: 42 ms, 0 net tps, 0 gross tps, height: 9, accepted txs: 0, rejected txs: 0, 
            root-hash: 46AF9064F12528CAD6A7C377204ACD0AC38CDC6912903E7DAB3703764C8DD5E5, 
            block-rid: 13779FAED9F36CB2411949E88D9D953FEC574ACB5676ABC4B9A69968A8FE8451, 
            prev-block-rid: 0000000000000000000000000000000000000000000000000000000000000000
            
            INFO  2019-11-20 14:00:34.175 [-1-BaseBlockDatabaseWorker] BaseBlockchainEngine - 
            [02:0C]/[D9:88] Loaded block: 42 ms, 0 net tps, 0 gross tps, height: 19, accepted txs: 0, rejected txs: 0, 
            root-hash: 46AF9064F12528CAD6A7C377204ACD0AC38CDC6912903E7DAB3703764C8DD5E5, 
            block-rid: 13779FAED9F36CB2411949E88D9D953FEC574ACB5676ABC4B9A69968A8FE8451, 
            prev-block-rid: 0000000000000000000000000000000000000000000000000000000000000000
        """.trimIndent()

        assert(parseLogLastHeight(log, "[D9:26]")).isEqualTo(8)
        assert(parseLogLastHeight(log, "[D9:99]")).isEqualTo(9)
    }

}