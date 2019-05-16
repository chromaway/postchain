package net.postchain.base

import net.postchain.base.data.DatabaseAccess
import net.postchain.core.ConfigurationDataStore
import net.postchain.core.EContext

object BaseConfigurationDataStore : ConfigurationDataStore {

    override fun findConfiguration(context: EContext, height: Long): Long? {
        return DatabaseAccess.of(context).findConfiguration(context, height)
    }

    override fun getConfigurationData(context: EContext, height: Long): ByteArray? {
        return DatabaseAccess.of(context).getConfigurationData(context, height)
    }

    override fun addConfigurationData(context: EContext, height: Long, data: ByteArray) {
        DatabaseAccess.of(context).addConfigurationData(context, height, data)
    }
}