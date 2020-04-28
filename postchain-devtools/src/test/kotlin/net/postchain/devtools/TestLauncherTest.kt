// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools

import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

@Ignore
class TestLauncherTest {

    private val blockchainRID = "101010101010101ABCDEF101010101010101ABCDEF101010101010101ABCDEF0"

    @Test
    fun runXMLGTXTests_nop() {
        val actual = true
        val expected = TestLauncher().runXMLGTXTests(
                readResourceFile("tx_nop.xml"),
                blockchainRID)

        assertEquals(expected.passed, actual)
    }

    @Test
    fun runXMLGTXTests_nop_no_blockchainRID() {
        val actual = true
        val expected = TestLauncher().runXMLGTXTests(
                readResourceFile("tx_nop_no_blockchainRID.xml"),
                blockchainRID)

        assertEquals(expected.passed, actual)
    }

    @Test
    fun runXMLGTXTests_timeb() {
        val actual = true
        val expected = TestLauncher().runXMLGTXTests(
                readResourceFile("tx_timeb.xml"),
                blockchainRID)

        assertEquals(expected.passed, actual)
    }

    @Test
    fun runXMLGTXTests_timeb_3x() {
        val actual = true
        val expected = TestLauncher().runXMLGTXTests(
                readResourceFile("tx_timeb_3x.xml"),
                blockchainRID)

        assertEquals(expected.passed, actual)
    }

    @Test
    fun runXMLGTXTests_block_single_empty_blocks() {
        val actual = true
        val expected = TestLauncher().runXMLGTXTests(
                readResourceFile("tx_block_single_empty_block.xml"),
                blockchainRID)

        assertEquals(expected.passed, actual)
    }

    @Test
    fun runXMLGTXTests_block_two_empty_blocks() {
        val actual = true
        val expected = TestLauncher().runXMLGTXTests(
                readResourceFile("tx_block_two_empty_blocks.xml"),
                blockchainRID)

        assertEquals(expected.passed, actual)
    }

    @Test
    fun runXMLGTXTests_block_two_blocks() {
        val actual = true
        val expected = TestLauncher().runXMLGTXTests(
                readResourceFile("tx_block_two_blocks.xml"),
                blockchainRID)

        assertEquals(expected.passed, actual)
    }

    private fun readResourceFile(filename: String): String {
        return javaClass.getResource("/net/postchain/devtools/test-launcher/$filename").readText()
    }
}