package fr.shikkanime.builders

import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.services.AbstractService
import fr.shikkanime.utils.Constant
import java.lang.reflect.ParameterizedType

abstract class AbstractEntityBuilder<E : ShikkEntity, S : AbstractService<E, *>> {
    @Suppress("UNCHECKED_CAST")
    private val serviceClass by lazy { (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<S> }
    private val service: S by lazy { Constant.injector.getInstance(serviceClass) }

    abstract fun buildEntity(): E

    open suspend fun build() = service.save(buildEntity())
}