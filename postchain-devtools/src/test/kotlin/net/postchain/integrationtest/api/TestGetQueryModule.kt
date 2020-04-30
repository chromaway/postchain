package net.postchain.integrationtest.api

import net.postchain.core.EContext
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory
import net.postchain.gtx.SimpleGTXModule

class TestGetQueryModule : SimpleGTXModule<Unit>(Unit,
        mapOf(),
        mapOf(
                "test_query" to { _, _, args ->
                    val flag = (args as GtvDictionary)["flag"]!!.asBoolean()
                    val number = args["i"]!!.asInteger()
                    if (flag) {
                        GtvFactory.gtv(number * number)
                    } else {
                        GtvFactory.gtv(number)
                    }
                }
        )
) {
    override fun initializeDB(ctx: EContext) {
    }
}