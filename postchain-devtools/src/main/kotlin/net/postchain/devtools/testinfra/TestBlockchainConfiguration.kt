package net.postchain.devtools.testinfra

import mu.KLogging
import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.core.EContext
import net.postchain.core.TransactionFactory

open class TestBlockchainConfiguration(configData: BaseBlockchainConfigurationData
) : BaseBlockchainConfiguration(configData) {

    val transactionFactory = TestTransactionFactory()

    companion object : KLogging()

    override fun getTransactionFactory(): TransactionFactory {
        return transactionFactory
    }

    override fun initializeDB(ctx: EContext) {
        super.initializeDB(ctx)
        logger.info("++ TEST ONLY ++: Running TestBlockchainConfiguration - means DB for modules NOT initialized! ++ TEST ONLY ++")
    }
}