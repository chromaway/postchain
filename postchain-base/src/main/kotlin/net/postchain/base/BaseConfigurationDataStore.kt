package net.postchain.base

import net.postchain.base.data.DatabaseAccess
import net.postchain.base.data.SQLDatabaseAccess
import net.postchain.core.ConfigurationDataStore
import net.postchain.core.EContext

class BaseConfigurationDataStore : ConfigurationDataStore {

    val db: DatabaseAccess = SQLDatabaseAccess()

    override fun getConfigurationData(context: EContext, height: Long): ByteArray {
        return db.getConfigurationData(context, height)
    }

    override fun addConfigurationData(context: EContext, height: Long, data: ByteArray) {
        db.addConfigurationData(context, height, data)
    }
}