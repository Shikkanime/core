package fr.shikkanime.utils

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.reflect.Type
import java.time.ZonedDateTime
import java.util.*

private class ZonedDateTimeAdapterDeserializer : JsonDeserializer<ZonedDateTime> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ZonedDateTime {
        return ZonedDateTime.parse(json.asJsonPrimitive.asString)
    }
}

private class ZonedDateTimeAdapterSerializer : JsonSerializer<ZonedDateTime> {
    override fun serialize(src: ZonedDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement? {
        return src?.let { context?.serialize(it.withUTCString()) }
    }
}

object ObjectParser {
    private val gson = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapterDeserializer())
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapterSerializer())
        .create()

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

    fun <T> fromBase64(base64: String): T {
        val bytes = Base64.getDecoder().decode(base64)

        return ObjectInputStream(bytes.inputStream()).use { ois ->
            @Suppress("UNCHECKED_CAST")
            ois.readObject() as T
        }
    }

    fun <T> toBase64(obj: T): String {
        ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos ->
                oos.writeObject(obj)
                return Base64.getEncoder().encodeToString(baos.toByteArray())
            }
        }
    }
}