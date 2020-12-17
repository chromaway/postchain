package net.postchain.base.data

object DatabaseAccessFactory {

    const val POSTGRES_DRIVER_CLASS = "org.postgresql.Driver"
    const val SAP_HANA_DRIVER_CLASS = "com.sap.db.jdbc.Driver"

    fun createDatabaseAccess(driverClassName: String): DatabaseAccess {
        return when (driverClassName) {
            POSTGRES_DRIVER_CLASS -> PostgreSQLDatabaseAccess()
            SAP_HANA_DRIVER_CLASS -> SAPHanaSQLDatabaseAccess()
            else -> throw Exception("Unknown database driver class detected: $driverClassName")
        }
    }

}