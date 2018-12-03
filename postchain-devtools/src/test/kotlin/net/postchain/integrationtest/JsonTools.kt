package net.postchain.integrationtest

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken

object JsonTools {

    fun buildGson(): Gson {
        return GsonBuilder()
                .registerTypeAdapter(Double::class.java, JsonSerializer<Double> { src, _, _ ->
                    // Avoid parsing integers as doubles
                    JsonPrimitive(Math.round(src!!).toInt())
                })
                .create()
    }

    fun jsonAsMap(gson: Gson, json: String): Map<String, Any> {
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(json, mapType)
    }
}