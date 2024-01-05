package fr.shikkanime.utils

import com.ctc.wstx.stax.WstxInputFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

object ObjectParser {
    private val gson = Gson()
    private val objectMapper = ObjectMapper()
    private val xmlInputFactory = WstxInputFactory()
    private val xmlMapper: XmlMapper

    init {
        xmlInputFactory.configureForSpeed()
        xmlInputFactory.setProperty(WstxInputFactory.IS_NAMESPACE_AWARE, false)
        xmlMapper = XmlMapper(xmlInputFactory)
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
        return this[key]?.asInt
    }

    fun JsonObject.getAsInt(key: String, default: Int): Int {
        return this[key]?.asString?.toIntOrNull() ?: default
    }

    fun JsonObject.getAsLong(key: String, default: Long): Long {
        return this[key]?.asLong ?: default
    }

    fun <T> fromXml(xml: String, clazz: Class<T>): T {
        return objectMapper.writeValueAsString(xmlMapper.readTree(xml)).let { gson.fromJson(it, clazz) }
    }
}