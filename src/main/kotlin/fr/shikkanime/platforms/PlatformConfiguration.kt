package fr.shikkanime.platforms

import io.ktor.http.*
import java.lang.reflect.Field
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Configuration(
    val label: String,
    val type: String,
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigurationConverter(
    val converter: KClass<out FieldConverter<*>>,
)

data class ConfigurationField(
    val label: String,
    val name: String,
    val type: String,
    val value: String,
)

interface FieldConverter<T : Any> {
    fun convert(value: String): T
    fun convert(value: Any): String
}

class SetStringFieldConverter(private val separator: String = ",") : FieldConverter<Set<String>> {
    override fun convert(value: String): Set<String> {
        return value.split(separator).toSet()
    }

    override fun convert(value: Any): String {
        return (value as Set<*>).joinToString(separator)
    }
}

class LongFieldConverter : FieldConverter<Long> {
    override fun convert(value: String): Long {
        return value.toLong()
    }

    override fun convert(value: Any): String {
        return value.toString()
    }
}

open class PlatformConfiguration(
    @Configuration(
        label = "Available countries (Country codes separated by commas)",
        type = "text",
    )
    @ConfigurationConverter(converter = SetStringFieldConverter::class)
    var availableCountries: MutableSet<String> = mutableSetOf(),
    @Configuration(
        label = "API check delay in minutes",
        type = "number",
    )
    @ConfigurationConverter(converter = LongFieldConverter::class)
    var apiCheckDelayInMinutes: Long = 0,
) {
    fun of(parameters: Parameters) {
        getAllFields()
            .filter { it.isAnnotationPresent(Configuration::class.java) }
            .forEach {
                it.isAccessible = true
                val converter = it.getAnnotation(ConfigurationConverter::class.java) ?: return@forEach
                val value = parameters[it.name]!!
                it[this] = converter.converter.java.getConstructor().newInstance().convert(value)
            }
    }

    fun toConfigurationFields(): MutableSet<ConfigurationField> {
        return getAllFields()
            .filter { it.isAnnotationPresent(Configuration::class.java) }
            .mapNotNull {
                it.isAccessible = true
                val configuration = it.getAnnotation(Configuration::class.java)
                val converter = it.getAnnotation(ConfigurationConverter::class.java) ?: return@mapNotNull null
                val value = it[this]!!

                ConfigurationField(
                    label = configuration.label,
                    name = it.name,
                    type = configuration.type,
                    value = converter.converter.java.getConstructor().newInstance().convert(value),
                )
            }.toMutableSet()
    }

    private fun getAllFields(): Set<Field> {
        return (this::class.java.declaredFields + this::class.java.superclass?.declaredFields.orEmpty()).toSet()
    }
}