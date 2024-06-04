package fr.shikkanime.utils

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.ZonedDateTime

object ObjectParser {
    class ZonedDateTimeAdapter : JsonDeserializer<ZonedDateTime> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ZonedDateTime {
            return ZonedDateTime.parse(json.asJsonPrimitive.asString)
        }
    }

    private val gson = GsonBuilder().registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter()).create()

    fun fromJson(json: String): JsonObject {
        return gson.fromJson(json, JsonObject::class.java)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }

    fun <T> fromJson(json: String, typeToken: TypeToken<T>): T {
        return gson.fromJson(json, typeToken.type)
    }

    fun <T> fromJson(jsonElement: JsonElement, clazz: Class<T>): T {
        return gson.fromJson(jsonElement, clazz)
    }

    fun <T> toJson(obj: T): String {
        return gson.toJson(obj)
    }

    fun JsonObject.getAsString(key: String): String? {
        return if (this[key] != null && !this[key].isJsonNull) this[key]?.asString else null
    }

    fun JsonObject.getAsBoolean(key: String): Boolean? {
        return this[key]?.asBoolean
    }

    fun JsonObject.getAsInt(key: String): Int? {
        return if (this[key] != null && !this[key].isJsonNull) this[key]?.asInt else null
    }

    fun JsonObject.getAsLong(key: String, default: Long): Long {
        return this[key]?.asLong ?: default
    }
}