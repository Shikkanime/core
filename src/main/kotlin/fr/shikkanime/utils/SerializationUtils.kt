package fr.shikkanime.utils

import com.google.gson.reflect.TypeToken

object SerializationUtils {
    enum class SerializationType {
        JSON,
        OBJECT
    }

    fun <T> deserialize(serializationType: SerializationType, string: String, typeToken: TypeToken<T>): T? {
        return when (serializationType) {
            SerializationType.JSON -> ObjectParser.fromJson(string, typeToken)
            SerializationType.OBJECT -> ObjectParser.fromBase64(string)
        }
    }

    fun <T> serialize(serializationType: SerializationType, `object`: T): String {
        return when (serializationType) {
            SerializationType.JSON -> ObjectParser.toJson(`object`)
            SerializationType.OBJECT -> ObjectParser.toBase64(`object`)
        }
    }
}