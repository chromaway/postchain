package net.postchain.base.data

import mu.KLogging
import net.postchain.base.BlockchainRelatedInfo
import net.postchain.core.BadDataMistake
import net.postchain.core.BadDataType
import net.postchain.core.EContext

object DependenciesValidator : KLogging() {

    fun validateBlockchainRids(ctx: EContext, dependencies: List<BlockchainRelatedInfo>) {
        val db = DatabaseAccess.of(ctx)

        // At this point we must have stored BC RID
        db.getBlockchainRid(ctx)
                ?: throw IllegalStateException("Cannot initialize block store for a chain without a RID")

        // Verify all dependencies
        for (dep in dependencies) {
            logger.debug("Validating")
            val chainId = db.getChainId(ctx, dep.blockchainRid)
            if (chainId == null) {
                throw BadDataMistake(BadDataType.BAD_CONFIGURATION,
                        "Dependency given in configuration: ${dep.nickname} is missing in DB. Dependent blockchains must be added in correct order!" +
                                " Dependency not found BC RID ${dep.blockchainRid.toHex()}")
            } else {
                logger.info("initialize() - Verified BC dependency: ${dep.nickname} exists as chainID: = $chainId (before: ${dep.chainId}) ")
                dep.chainId = chainId
            }
        }
    }

}