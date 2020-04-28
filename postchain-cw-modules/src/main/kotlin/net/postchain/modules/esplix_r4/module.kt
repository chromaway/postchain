// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.modules.esplix_r4

import net.postchain.base.BlockchainRid
import net.postchain.core.EContext
import net.postchain.gtv.Gtv
import net.postchain.gtx.GTXModule
import net.postchain.gtx.GTXModuleFactory
import net.postchain.gtx.GTXSchemaManager
import net.postchain.gtx.SimpleGTXModule

class EsplixModule(val config: EsplixConfig) : SimpleGTXModule<EsplixConfig>(
        config,
        mapOf(
                "R4createChain" to ::create_chain_op,
                "R4postMessage" to ::post_message_op
        ),
        mapOf(
                "R4getMessages" to ::getMessagesQ,
                "R4getTxRID" to ::getTxRIDQ
        )
) {

    override fun initializeDB(ctx: EContext) {
        GTXSchemaManager.autoUpdateSQLSchema(
                ctx, 0, javaClass, "schema.sql", "chromaway.esplix_r4"
        )
    }
}

class BaseEsplixModuleFactory : GTXModuleFactory {
    override fun makeModule(data: Gtv, blockchainRID: BlockchainRid): GTXModule {
        return EsplixModule(makeBaseEsplixConfig(data, blockchainRID.data))
    }
}
