// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.base.data

object SQLCommandsFactory {

    private val POSTGRE_DRIVER_CLASS = "org.postgresql.Driver"
    private val SAP_HANA_DRIVER_CLASS = "com.sap.db.jdbc.Driver"

    fun getSQLCommands(driverClassName: String): SQLCommands {
        return when (driverClassName) {
            POSTGRE_DRIVER_CLASS -> PostgreSQLCommands
            SAP_HANA_DRIVER_CLASS -> SAPHanaSQLCommands
            else -> PostgreSQLCommands
        }
    }
}