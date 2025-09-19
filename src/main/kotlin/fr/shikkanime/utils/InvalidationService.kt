package fr.shikkanime.utils

import io.ktor.util.collections.*
import kotlin.reflect.KClass

abstract class InvalidationService(open val classes: List<Class<*>>) {
    fun register(name: String, invalidationService: InvalidationService) {
        globalCaches["$name:${invalidationService::class.simpleName}"] = this
    }

    abstract fun invalidate()

    companion object {
        private val globalCaches: ConcurrentMap<String, InvalidationService> = ConcurrentMap()

        fun invalidate(vararg classes: Class<*>) {
            globalCaches.forEach { (_, cache) ->
                if (cache.classes.any { clazz -> clazz in classes })
                    cache.invalidate()
            }

            AsynchronizedGlideClient.del()
        }

        fun invalidateAll() {
            globalCaches.forEach { (_, cache) ->
                cache.invalidate()
            }

            AsynchronizedGlideClient.del()
        }

        fun <T : InvalidationService> getByNameAndType(
            name: String,
            type: KClass<out T>
        ) = globalCaches["$name:${type.simpleName}"]
    }
}