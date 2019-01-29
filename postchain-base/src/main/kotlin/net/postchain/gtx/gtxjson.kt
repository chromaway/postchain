// Copyright (c) 2017 ChromaWay Inc. See README for license information.

package net.postchain.gtx

import net.postchain.common.toHex
import net.postchain.core.ProgrammerMistake
import com.google.gson.*
import java.lang.reflect.Type
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.*

class GtvAdapter : JsonDeserializer<Gtv>, JsonSerializer<Gtv> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Gtv {
        if (json.isJsonPrimitive) {
            val prim = json.asJsonPrimitive
            if (prim.isBoolean)
                return gtv(if (prim.asBoolean) 1L else 0L)
            else if (prim.isNumber)
                return gtv(prim.asLong)
            else if (prim.isString)
                return gtv(prim.asString)
            else throw ProgrammerMistake("Can't deserialize JSON primitive")
        } else if (json.isJsonArray) {
            val arr = json.asJsonArray
            return gtv(*arr.map({ deserialize(it, typeOfT, context) }).toTypedArray())
        } else if (json.isJsonNull) {
            return GtvNull
        } else if (json.isJsonObject) {
            val obj = json.asJsonObject
            val mut = mutableMapOf<String, Gtv>()
            obj.entrySet().forEach {
                mut[it.key] = deserialize(it.value, typeOfT, context)
            }
            return gtv(mut)
        } else throw ProgrammerMistake("Could not deserialize JSON element")
    }

    private fun encodeDict(d: Gtv, t: Type, c: JsonSerializationContext): JsonObject {
        val o = JsonObject()
        for ((k, v) in d.asDict()) {
            o.add(k, serialize(v, t, c))
        }
        return o
    }

    private fun encodeArray(d: Gtv, t: Type, c: JsonSerializationContext): JsonArray {
        val a = JsonArray()
        for (v in d.asArray()) {
            a.add(serialize(v, t, c))
        }
        return a
    }

    override fun serialize(v: Gtv, t: Type, c: JsonSerializationContext): JsonElement {
        when (v.type) {
            GtvType.INTEGER -> return JsonPrimitive(v.asInteger())
            GtvType.STRING -> return JsonPrimitive(v.asString())
            GtvType.NULL -> return JsonNull.INSTANCE
            GtvType.BYTEARRAY -> return JsonPrimitive(v.asByteArray().toHex())
            GtvType.DICT -> return encodeDict(v, t, c)
            GtvType.ARRAY -> return encodeArray(v, t, c)
            else -> throw NotImplementedError("TODO") // TODO: fix
        }
    }
}

fun make_gtx_gson(): Gson {
    return GsonBuilder().
            registerTypeAdapter(Gtv::class.java, GtvAdapter()).
            serializeNulls().
            create()!!
}

fun gtxToJSON(Gtv: Gtv, gson: Gson): String {
    return gson.toJson(Gtv, Gtv::class.java)
}