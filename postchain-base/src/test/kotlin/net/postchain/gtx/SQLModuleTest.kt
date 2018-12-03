package net.postchain.gtx

import net.postchain.StorageBuilder
import net.postchain.base.withWriteConnection
import org.apache.commons.configuration2.MapConfiguration
import org.junit.Assert
import org.junit.Test
import java.nio.file.Paths

class SQLModuleTest {

    @Test
    fun testModule() {
        val moduleFileName = Paths.get(javaClass.getResource("sqlmodule1.sql").toURI()).toString()
        val config = gtx(
                "gtx" to gtx("sqlmodules" to gtx(gtx(moduleFileName)))
        )

        val mf = SQLGTXModuleFactory()
        val module = mf.makeModule(config, testBlockchainRID)

        val dataConf = MapConfiguration(mapOf(
                "database.driverclass" to "org.postgresql.Driver",
                "database.url" to "jdbc:postgresql://localhost/postchain",
                "database.username" to "postchain",
                "database.password" to "postchain",
                "database.schema" to "testschema"
        ))

        val storage = StorageBuilder.buildStorage(dataConf, 0)
        withWriteConnection(storage, 1) {
            GTXSchemaManager.initializeDB(it)
            module.initializeDB(it)
            Assert.assertTrue(module.getOperations().size == 1)
            false
        }
    }
}