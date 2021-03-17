package net.postchain.base

import assertk.assert
import assertk.assertions.isEqualTo
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import net.postchain.core.BlockQueries
import net.postchain.core.TransactionQueue
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import nl.komponents.kovenant.Promise
import org.awaitility.Awaitility.await
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.TimeUnit

class BaseBlockBuildingStrategyTest {

    @Test
    fun test_before_POS131() {
        // Mock
        val strategyData: Gtv = mock() {
            on { get(eq("maxblocktime")) } doReturn GtvFactory.gtv(2000L)
            on { get(eq("blockdelay")) } doReturn GtvFactory.gtv(2000L)
            on { get(eq("maxblocktransactions")) } doReturn GtvFactory.gtv(100L)
        }

        val configData: BaseBlockchainConfigurationData = mock {
            on { getBlockBuildingStrategy() } doReturn strategyData
        }

        val height: Promise<Long, Exception> = mock {
            onGeneric { get() } doReturn -1L
        }
        val blockQueries: BlockQueries = mock {
            on { getBestHeight() } doReturn height
        }

        val txQueue: TransactionQueue = mock {
            on { getTransactionQueueSize() } doReturn 0
        }

        val sut = BaseBlockBuildingStrategy_POS130(
                configData, mock(), blockQueries, txQueue
        )

        // When
        await().atLeast(1900, TimeUnit.MILLISECONDS).untilAsserted {
            assert(sut.shouldBuildBlock()).isEqualTo(true)
        }
    }

    @Test @Ignore
    fun test_after_POS131() {
        // Mock
        val strategyData: Gtv = mock() {
            on { get(eq("maxblocktime")) } doReturn GtvFactory.gtv(2000L)
            on { get(eq("blockdelay")) } doReturn GtvFactory.gtv(2000L)
            on { get(eq("maxblocktransactions")) } doReturn GtvFactory.gtv(100L)
        }

        val configData: BaseBlockchainConfigurationData = mock {
            on { getBlockBuildingStrategy() } doReturn strategyData
        }

        val height: Promise<Long, Exception> = mock {
            onGeneric { get() } doReturn -1L
        }
        val blockQueries: BlockQueries = mock {
            on { getBestHeight() } doReturn height
        }

        val txQueue: TransactionQueue = mock {
            on { getTransactionQueueSize() } doReturn 0
        }

        val sut = BaseBlockBuildingStrategy(
                configData, mock(), blockQueries, txQueue
        )

        // When
        await().atLeast(1900, TimeUnit.MILLISECONDS).untilAsserted {
            assert(sut.shouldBuildBlock()).isEqualTo(true)
        }
    }

}