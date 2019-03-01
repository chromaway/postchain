// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.ebft.messages

import net.postchain.ebft.message.BlockSignature
import net.postchain.ebft.message.GetBlockAtHeight
import net.postchain.ebft.message.Message
import net.postchain.ebft.message.Signature
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MessagesTest {
    @Test
    fun testGetBlockAtHeight() {
        val mess = GetBlockAtHeight(29)
        val encoded = mess.encode()

        val result = Message.decode(encoded) as net.postchain.ebft.message.GetBlockAtHeight
        assertEquals(mess.height, result.height)
    }

    @Test
    fun testBlockSignature() {
        val blockRID = ByteArray(32){it.toByte()}
        val subjectID = ByteArray(33) {it.toByte()}
        val data = ByteArray(40){(it+1).toByte()}
        val sig = Signature(subjectID, data)
        val mess = BlockSignature(blockRID, sig)
        val encoded = mess.encode()

        val result = Message.decode(encoded) as BlockSignature
        assertArrayEquals(mess.blockRID, result.blockRID)
        assertArrayEquals(mess.sig.subjectID, result.sig.subjectID)
        assertArrayEquals(mess.sig.data, result.sig.data)
    }
}