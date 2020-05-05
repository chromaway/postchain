// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.modules.certificate

import net.postchain.base.BlockchainRid
import net.postchain.core.EContext
import net.postchain.gtv.Gtv
import net.postchain.gtx.GTXModule
import net.postchain.gtx.GTXModuleFactory
import net.postchain.gtx.GTXSchemaManager
import net.postchain.gtx.SimpleGTXModule

class CertificateModule(val config: CertificateConfig) : SimpleGTXModule<CertificateConfig>(
        config,
        mapOf(
                "certificate" to ::certificate_op
        ),
        mapOf(
                "get_certificates" to ::getCertificatesQ
        )
) {

    override fun initializeDB(ctx: EContext) {
        GTXSchemaManager.autoUpdateSQLSchema(
                ctx, 0, javaClass, "schema.sql", "chromaway.certificate"
        )
    }
}

class BaseCertificateModuleFactory : GTXModuleFactory {
    override fun makeModule(data: Gtv, blockchianRID: BlockchainRid): GTXModule {
        return CertificateModule(makeBaseCertificateConfig(data, blockchianRID))
    }
}
