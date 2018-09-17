package net.postchain.test.testinfra

import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.core.TransactionFactory

open class TestBlockchainConfiguration(configData: BaseBlockchainConfigurationData
) : BaseBlockchainConfiguration(configData) {

    val transactionFactory = TestTransactionFactory()

    override fun getTransactionFactory(): TransactionFactory {
        return transactionFactory
    }
}