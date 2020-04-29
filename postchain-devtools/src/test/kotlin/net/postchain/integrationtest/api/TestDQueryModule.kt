package net.postchain.integrationtest.api

import net.postchain.core.EContext
import net.postchain.core.UserMistake
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory
import net.postchain.gtx.SimpleGTXModule

class TestDQueryModule : SimpleGTXModule<Unit>(Unit,
        mapOf(),
        mapOf(
                "get_front_page" to { _, _, args ->
                    (args as GtvDictionary)["id"]
                            ?: throw UserMistake("get_front_page can not take id as null")
                    GtvFactory.gtv(
                            GtvFactory.gtv("text/html"),
                            GtvFactory.gtv("<h1>it works!</h1>")
                    )
                },

                "get_picture" to { _, _, args ->
                    (args as GtvDictionary)["id"]
                            ?: throw UserMistake("get_picture can not take id as null")
                    GtvFactory.gtv(
                            GtvFactory.gtv("image/png"),
                            GtvFactory.gtv("abcd".toByteArray())
                    )
                }
        )
) {
    override fun initializeDB(ctx: EContext) {
    }
}
