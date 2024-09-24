package fr.shikkanime.converters

import fr.shikkanime.utils.Constant
import java.lang.reflect.ParameterizedType
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

abstract class AbstractConverter<F, T> {
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Converter

    companion object {
        val converters: MutableMap<Pair<Class<*>, Class<*>>, AbstractConverter<*, *>> = mutableMapOf()

        init {
            val converters = Constant.reflections.getSubTypesOf(AbstractConverter::class.java)

            converters.forEach {
                val (from, to) = (it.genericSuperclass as ParameterizedType).actualTypeArguments.map { argument -> argument as Class<*> }
                this.converters[Pair(from, to)] = Constant.injector.getInstance(it)
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

            val abstractConverter = converters[pair] ?: throw IllegalStateException()
            val function = abstractConverter::class.declaredFunctions.firstOrNull { it.annotations.any { annotation -> annotation is Converter } }
                ?: throw NoSuchElementException("Can not find converter function for \"${`object`.javaClass.simpleName}\" to \"${to.simpleName}\"")
            function.isAccessible = true

            return try {
                val invoke = function.call(abstractConverter, `object`, *args)
                    ?: throw NullPointerException("Can not convert null to \"${to.simpleName}\"")
                check(invoke is T) { "Can not convert \"${`object`.javaClass.simpleName}\" to \"${to.simpleName}\"" }
                invoke
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

        inline fun <reified T> convert(set: Set<Any>?, to: Class<T>): Set<T>? {
            return set?.map { convert(it, to) }?.toSet()
        }
    }
}