package net.postchain.base.data

object DatabaseAccessFactory {

    const val POSTGRES_DRIVER_CLASS = "org.postgresql.Driver"

    fun createDatabaseAccess(driverClassName: String): DatabaseAccess {
        return when (driverClassName) {
            POSTGRES_DRIVER_CLASS -> PostgreSQLDatabaseAccess()
            else -> throw Exception("Unknown database driver class detected: $driverClassName")
        }
    }

}