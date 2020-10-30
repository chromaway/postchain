// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.gtx

import mu.KLogging
import net.postchain.base.*
import net.postchain.base.data.BaseBlockchainConfiguration
import net.postchain.core.*
import net.postchain.gtv.Gtv
import net.postchain.gtv.gtvToJSON
import net.postchain.gtv.make_gtv_gson
import nl.komponents.kovenant.Promise

open class GTXBlockchainConfiguration(configData: BaseBlockchainConfigurationData, val module: GTXModule)
    : BaseBlockchainConfiguration(configData) {
    private val txFactory = GTXTransactionFactory(
            effectiveBlockchainRID, module, cryptoSystem, configData.getMaxTransactionSize())
    private val specTxHandler = GTXSpecialTxHandler(module, effectiveBlockchainRID, cryptoSystem,
            txFactory
    )

    companion object : KLogging()

    override fun getTransactionFactory(): TransactionFactory {
        return txFactory
    }

    override fun getSpecialTxHandler(): SpecialTransactionHandler {
        return specTxHandler
    }

    override fun initializeDB(ctx: EContext) {
        super.initializeDB(ctx)
        logger.debug("Running initialize DB of class GTXBlockchainConfiguration")
        GTXSchemaManager.initializeDB(ctx)
        module.initializeDB(ctx)
    }

    override fun makeBlockQueries(storage: Storage): BlockQueries {
        return object : BaseBlockQueries(this@GTXBlockchainConfiguration, storage, blockStore,
                chainID, configData.subjectID) {
            private val gson = make_gtv_gson()

            override fun query(query: String): Promise<String, Exception> {
                val gtxQuery = gson.fromJson<Gtv>(query, Gtv::class.java)
                return runOp {
                    val type = gtxQuery.asDict()["type"] ?: throw UserMistake("Missing query type")
                    val queryResult = module.query(it, type.asString(), gtxQuery)
                    gtvToJSON(queryResult, gson)
                }
            }

            override fun query(name: String, args: Gtv): Promise<Gtv, Exception> {
                return runOp {
                    module.query(it, name, args)
                }
            }

        }
    }
}

open class GTXBlockchainConfigurationFactory : BlockchainConfigurationFactory {

    override fun makeBlockchainConfiguration(configurationData: Any): BlockchainConfiguration {
        val cfData = configurationData as BaseBlockchainConfigurationData
        val effectiveBRID = cfData.getHistoricBRID() ?: configurationData.context.blockchainRID
        return GTXBlockchainConfiguration(
                cfData,
                createGtxModule(effectiveBRID, configurationData.data))
    }

    open fun createGtxModule(blockchainRID: BlockchainRid, data: Gtv): GTXModule {
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