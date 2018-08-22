// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.gtx

import net.postchain.base.BaseBlockQueries
import net.postchain.base.BaseBlockchainConfigurationData
import net.postchain.base.Storage
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.core.*
import nl.komponents.kovenant.Promise

open class GTXBlockchainConfiguration(configData: BaseBlockchainConfigurationData, val module: GTXModule)
    : BaseBlockchainConfiguration(configData) {
    val txFactory = GTXTransactionFactory(blockchainRID, module, cryptoSystem)

    override fun getTransactionFactory(): TransactionFactory {
        return txFactory
    }

    override fun initializeDB(ctx: EContext) {
        super.initializeDB(ctx)
        GTXSchemaManager.initializeDB(ctx)
        module.initializeDB(ctx)
    }

    override fun makeBlockQueries(storage: Storage): BlockQueries {
        return object : BaseBlockQueries(this@GTXBlockchainConfiguration, storage, blockStore,
                chainID, configData.subjectID) {
            private val gson = make_gtx_gson()

            override fun query(query: String): Promise<String, Exception> {
                val gtxQuery = gson.fromJson<GTXValue>(query, GTXValue::class.java)
                return runOp {
                    val type = gtxQuery.asDict().get("type") ?: throw UserMistake("Missing query type")
                    val queryResult = module.query(it, type.asString(), gtxQuery)
                    gtxToJSON(queryResult, gson)
                }
            }
        }
    }
}

open class GTXBlockchainConfigurationFactory : BlockchainConfigurationFactory {
    override fun makeBlockchainConfiguration(configurationData: Any, context: BlockchainContext): BlockchainConfiguration {
        return GTXBlockchainConfiguration(configurationData as BaseBlockchainConfigurationData,
                createGtxModule(configurationData.blockchainRID, configurationData.data))
    }

    open fun createGtxModule(blockchainRID: ByteArray, data: GTXValue): GTXModule {
        val gtxConfig = data["gtx"]!!
        val list = gtxConfig["modules"]!!.asArray().map { it.asString() }
        if (list.isEmpty()) {
            throw UserMistake("Missing GTX module in config. expected property 'blockchain.<chainId>.gtx.modules'")
        }

        fun makeModule(name: String): GTXModule {
            val moduleClass = Class.forName(name)
            val instance = moduleClass.newInstance()
            if (instance is GTXModule) {
                return instance
            } else if (instance is GTXModuleFactory) {
                return instance.makeModule(data, blockchainRID) //TODO
            } else throw UserMistake("Module class not recognized")
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