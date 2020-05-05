// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools.testinfra

import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.base.BlockchainRid
import net.postchain.core.BlockchainConfiguration
import net.postchain.core.BlockchainConfigurationFactory
import net.postchain.core.UserMistake
import net.postchain.gtv.Gtv
import net.postchain.gtx.CompositeGTXModule
import net.postchain.gtx.GTXModule
import net.postchain.gtx.GTXModuleFactory

open class TestBlockchainConfigurationFactory : BlockchainConfigurationFactory {

    override fun makeBlockchainConfiguration(configData: Any): BlockchainConfiguration {
        return TestBlockchainConfiguration(
                configData as BaseBlockchainConfigurationData,
                createGtxModule(configData.context.blockchainRID, configData.data)
        )
    }

    // FYI: Copied from GTXBlockchainConfigurationFactory.createGtxModule()
    protected fun createGtxModule(blockchainRID: BlockchainRid, data: Gtv): GTXModule {
        val gtxConfig = data["gtx"]!!
        val list = gtxConfig["modules"]!!.asArray().map { it.asString() }
        if (list.isEmpty()) {
            throw UserMistake("Missing GTX module in config. expected property 'blockchain.<chainId>.gtx.modules'")
        }

        fun makeModule(name: String): GTXModule {
            val moduleClass = Class.forName(name)
            val instance = moduleClass.newInstance()
            return when (instance) {
                is GTXModule -> instance
                is GTXModuleFactory -> instance.makeModule(data, blockchainRID) //TODO
                else -> throw UserMistake("Module class not recognized")
            }
        }

        return if (list.size == 1) {
            makeModule(list[0])
        } else {
            val moduleList = list.map(::makeModule)
            val allowOverrides = (gtxConfig["allowoverrides"]?.asInteger() ?: 0L) == 0L
            CompositeGTXModule(moduleList.toTypedArray(), allowOverrides)
        }
    }
}