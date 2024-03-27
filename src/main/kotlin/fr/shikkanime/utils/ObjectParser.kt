package fr.shikkanime.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

object ObjectParser {
    private val gson = Gson()

    fun fromJson(json: String): JsonObject {
        return gson.fromJson(json, JsonObject::class.java)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }

    fun <T> fromJson(json: String, typeToken: TypeToken<T>): T {
        return gson.fromJson(json, typeToken.type)
    }

    fun <T> toJson(obj: T): String {
        return gson.toJson(obj)
    }

    fun JsonObject.getAsString(key: String): String? {
        return if (this[key] != null && !this[key].isJsonNull) this[key]?.asString else null
    }

    fun JsonObject.getNullableJsonObject(key: String): JsonObject? {
        return if (this[key] != null && !this[key].isJsonNull) this[key]?.asJsonObject else null
    }

    fun JsonObject.getAsBoolean(key: String): Boolean? {
        return this[key]?.asBoolean
    }

    fun JsonObject.getAsInt(key: String): Int? {
        return if (this[key] != null && !this[key].isJsonNull) this[key]?.asInt else null
    }

    fun JsonObject.getAsInt(key: String, default: Int): Int {
        return this[key]?.asString?.toIntOrNull() ?: default
    }

    fun JsonObject.getAsLong(key: String, default: Long): Long {
        return this[key]?.asLong ?: default
    }

    fun JsonObject.getAsBoolean(key: String, default: Boolean): Boolean {
        return this[key]?.asBoolean ?: default
    }
}