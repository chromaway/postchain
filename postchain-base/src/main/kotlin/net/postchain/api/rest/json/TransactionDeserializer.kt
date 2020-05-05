// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.api.rest.json

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.postchain.api.rest.model.ApiTx
import net.postchain.core.UserMistake
import java.lang.reflect.Type

internal class TransactionDeserializer : JsonDeserializer<ApiTx> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ApiTx =
            (json as? JsonObject)
                    ?.run { ApiTx(get("tx")!!.asString) }
                    ?: throw UserMistake("Can't parse tx")
}