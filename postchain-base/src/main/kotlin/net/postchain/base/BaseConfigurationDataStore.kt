package net.postchain.base

import net.postchain.base.data.DatabaseAccess
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.core.ConfigurationDataStore
import net.postchain.core.EContext

object BaseConfigurationDataStore : ConfigurationDataStore {

    private val db: DatabaseAccess = SQLDatabaseAccess()

    override fun findConfiguration(context: EContext, height: Long): ByteArray? {
        return db.findConfiguration(context, height)
    }

    override fun getConfigurationData(context: EContext, height: Long): ByteArray? {
        return db.getConfigurationData(context, height)
    }

    override fun addConfigurationData(context: EContext, height: Long, data: ByteArray): Long {
        return db.addConfigurationData(context, height, data)
    }
}