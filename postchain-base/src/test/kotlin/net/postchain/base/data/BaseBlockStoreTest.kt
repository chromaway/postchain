// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

import net.postchain.base.BaseEContext
import net.postchain.base.BlockchainRid
import net.postchain.base.SECP256K1CryptoSystem
import net.postchain.core.EContext
import org.easymock.EasyMock.*
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import java.sql.Connection

class BaseBlockStoreTest {

    val cryptoSystem = SECP256K1CryptoSystem()
    val blockchainRID = BlockchainRid(cryptoSystem.digest("Test BlockchainRID".toByteArray()))
    lateinit var sut: BaseBlockStore
    lateinit var db: DatabaseAccess
    lateinit var ctx: EContext

    @Before
    fun setup() {
        sut = BaseBlockStore()
        db = mock(DatabaseAccess::class.java)
        //sut.db = db
        ctx = BaseEContext(mock(Connection::class.java), 2L, 0, db)
    }

    @Test
    fun beginBlockReturnsBlockchainRIDOnFirstBlock() {
        expect(db.getLastBlockHeight(ctx)).andReturn(-1)
        expect(db.getBlockchainRid(ctx)).andReturn(blockchainRID)
        expect(db.insertBlock(ctx, 0)).andReturn(17)
        expect(db.getLastBlockTimestamp(ctx)).andReturn(1509606236)
        replay(db)
        val initialBlockData = sut.beginBlock(ctx, blockchainRID, null)
        assertArrayEquals(blockchainRID.data, initialBlockData.prevBlockRID)
    }

    @Test
    fun beginBlockReturnsPrevBlockRIdOnSecondBlock() {
        val anotherRID = cryptoSystem.digest("A RID".toByteArray())
        expect(db.getLastBlockHeight(ctx)).andReturn(0)
        expect(db.getBlockRID(ctx, 0)).andReturn(anotherRID)
        expect(db.getBlockchainRid(ctx)).andReturn(blockchainRID)
        expect(db.insertBlock(ctx, 1)).andReturn(17)
        expect(db.getLastBlockTimestamp(ctx)).andReturn(1509606236)
        replay(db)
        val initialBlockData = sut.beginBlock(ctx, blockchainRID, null)
        assertArrayEquals(anotherRID, initialBlockData.prevBlockRID)
    }
}