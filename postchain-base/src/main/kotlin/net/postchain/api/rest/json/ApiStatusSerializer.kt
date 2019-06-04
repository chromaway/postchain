package net.postchain.api.rest.json

import com.google.gson.*
import net.postchain.api.rest.model.ApiStatus
import java.lang.reflect.Type

internal class ApiStatusSerializer : JsonSerializer<ApiStatus> {

    override fun serialize(src: ApiStatus?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement =
            JsonObject().apply {
                add("status", JsonPrimitive(src!!.status))
                src.rejectReason?.let {
                    add("rejectReason", JsonPrimitive(src.rejectReason)) }
            }
}
