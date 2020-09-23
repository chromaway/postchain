// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base

import mu.KLogging
import net.postchain.base.data.DatabaseAccess
import net.postchain.core.ConfigurationDataStore
import net.postchain.core.EContext
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder

object BaseConfigurationDataStore : KLogging(), ConfigurationDataStore {

    override fun findConfigurationHeightForBlock(context: EContext, height: Long): Long? {
        return DatabaseAccess.of(context).findConfigurationHeightForBlock(context, height)
    }

    override fun getConfigurationData(context: EContext, height: Long): ByteArray? {
        return DatabaseAccess.of(context).getConfigurationData(context, height)
    }

    override fun addConfigurationData(context: EContext, height: Long, binData: ByteArray) {
        return DatabaseAccess.of(context).addConfigurationData(
                context, height, binData)
    }

    override fun addConfigurationData(context: EContext, height: Long, gtvData: Gtv) {
        DatabaseAccess.of(context).addConfigurationData(
                context, height, GtvEncoder.encodeGtv(gtvData))
    }
}