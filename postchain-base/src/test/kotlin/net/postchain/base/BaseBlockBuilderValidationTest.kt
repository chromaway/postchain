// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import net.postchain.base.data.BaseBlockBuilder
import net.postchain.base.data.BaseBlockStore
import net.postchain.base.data.BaseTransactionFactory
import net.postchain.base.data.DatabaseAccess
import net.postchain.common.hexStringToByteArray
import net.postchain.core.InitialBlockData
import net.postchain.core.ValidationResult.Result.*
import net.postchain.devtools.KeyPairHelper.privKey
import net.postchain.devtools.KeyPairHelper.pubKey
import net.postchain.devtools.MockCryptoSystem
import org.easymock.EasyMock.mock
import org.junit.Test
import java.sql.Connection
import kotlin.test.assertEquals

class BaseBlockBuilderValidationTest {
    // Mocks
    val cryptoSystem = MockCryptoSystem()
    val mockedConn = mock(Connection::class.java)

    // Real stuff
    var bbs = BaseBlockStore()
    val tf = BaseTransactionFactory()
    val myBlockchainRid = BlockchainRid.ZERO_RID
    val empty32Bytes = ByteArray(32, { 0 })
    val rootHash = "46AF9064F12528CAD6A7C377204ACD0AC38CDC6912903E7DAB3703764C8DD5E5".hexStringToByteArray()
    val badRootHash = "46AF9064F12FFFFFFFFFFFFFF04ACD0AC38CDC6912903E7DAB3703764C8DD5E5".hexStringToByteArray()
    val subjects = arrayOf("test".toByteArray())
    val sigMaker = cryptoSystem.buildSigMaker(pubKey(0), privKey(0))

    // Objects using mocks
    val db = mock(DatabaseAccess::class.java)
    val ctx = BaseEContext(mockedConn, 2L, 0, db)
    val bctx = BaseBlockEContext(ctx, 0, 1, 10, mapOf())
    val bbb = BaseBlockBuilder(BlockchainRid.buildRepeat(0), cryptoSystem, ctx, bbs, tf,
            NullSpecialTransactionHandler(),
            subjects, sigMaker, listOf(), false)

    @Test
    fun validateBlockHeader_valid() {
        val timestamp = 100L
        val blockData = InitialBlockData(myBlockchainRid, 2, 2, empty32Bytes, 1, timestamp, null)
        val header = BaseBlockHeader.make(cryptoSystem, blockData, rootHash, timestamp)
        bbb.bctx = bctx
        bbb.initialBlockData = blockData

        val validation = bbb.validateBlockHeader(header)

        assertEquals(OK, validation.result)
    }

    @Test
    fun validateBlockHeader_invalidMonotoneTimestamp() {
        val timestamp = 1L
        val blockData = InitialBlockData(myBlockchainRid,2, 2, empty32Bytes, 1, timestamp, null)
        val header = BaseBlockHeader.make(cryptoSystem, blockData, rootHash, timestamp)
        bbb.bctx = bctx
        bbb.initialBlockData = blockData

        val validation = bbb.validateBlockHeader(header)

        assertEquals(INVALID_TIMESTAMP, validation.result)
    }

    @Test
    fun validateBlockHeader_invalidMonotoneTimestampEquals() {
        val timestamp = 10L
        val blockData = InitialBlockData(myBlockchainRid,2, 2, empty32Bytes, 1, timestamp, null)
        val header = BaseBlockHeader.make(cryptoSystem, blockData, rootHash, timestamp)
        bbb.bctx = bctx
        bbb.initialBlockData = blockData

        val validation = bbb.validateBlockHeader(header)

        assertEquals(INVALID_TIMESTAMP, validation.result)
    }

    @Test
    fun validateBlokcHeader_invalidRootHash() {
        val timestamp = 100L
        val blockData = InitialBlockData(myBlockchainRid,2, 2, empty32Bytes, 1, timestamp, null)
        val header = BaseBlockHeader.make(cryptoSystem, blockData, badRootHash, timestamp)
        bbb.bctx = bctx
        bbb.initialBlockData = blockData

        val validation = bbb.validateBlockHeader(header)

        assertEquals(INVALID_ROOT_HASH, validation.result)
    }

}
