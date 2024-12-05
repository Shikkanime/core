package fr.shikkanime.converters

import fr.shikkanime.utils.Constant
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.isAccessible

abstract class AbstractConverter<F, T> {
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Converter

    companion object {
        val converters: MutableMap<Pair<Class<*>, Class<*>>, Pair<AbstractConverter<*, *>, KFunction<*>>> = mutableMapOf()

        init {
            val converters = Constant.reflections.getSubTypesOf(AbstractConverter::class.java)

            converters.forEach {
                val (from, to) = (it.genericSuperclass as ParameterizedType).actualTypeArguments.filterIsInstance<Class<*>>()
                val abstractConverter = Constant.injector.getInstance(it)
                val function = abstractConverter::class.declaredFunctions.firstOrNull { it.hasAnnotation<Converter>() }
                    ?: throw NoSuchElementException("Can not find converter function for \"${from.simpleName}\" to \"${to.simpleName}\"")
                function.isAccessible = true
                this.converters[Pair(from, to)] = abstractConverter to function
            }
        }

        inline fun <reified T> convert(`object`: Any?, to: Class<T>, vararg args: Any): T {
            if (`object` == null) {
                throw NullPointerException("Can not convert null to \"${to.simpleName}\"")
            }

            val pair = Pair(`object`.javaClass, to)

            if (!converters.containsKey(pair)) {
                throw NoSuchElementException("Can not find converter \"${`object`.javaClass.simpleName}\" to \"${to.simpleName}\"")
            }

            val (abstractConverter, function) = converters[pair] ?: throw IllegalStateException()

            return try {
                function.call(abstractConverter, `object`, *args) as? T ?: throw NullPointerException("Can not convert null to \"${to.simpleName}\"")
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Can not convert \"${`object`.javaClass.simpleName}\" to \"${to.simpleName}\"",
                    e
                )
            }
        }

        inline fun <reified T> convert(list: Collection<Any>?, to: Class<T>): List<T>? {
            return list?.map { convert(it, to) }
        }

        inline fun <reified T> convert(set: Set<Any>?, to: Class<T>): MutableSet<T>? {
            return set?.map { convert(it, to) }?.toMutableSet()
        }
    }
}