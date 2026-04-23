package fr.shikkanime.modules

import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.entities.Tracing
import jakarta.persistence.PrePersist
import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

class EntityLifecycleListener {
    private val traceActionService: TraceActionService by lazy { Constant.injector.getInstance(TraceActionService::class.java) }

    @PrePersist
    fun onEntityCreate(entity: Any) {
        if (entity !is ShikkEntity || !entity::class.hasAnnotation<Tracing>())
            return

        traceActionService.createTraceAction(entity, TraceAction.Action.CREATE)
    }

    @PreUpdate
    fun onEntityUpdate(entity: Any) {
        if (entity !is ShikkEntity || !entity::class.hasAnnotation<Tracing>())
            return

        traceActionService.createTraceAction(entity, TraceAction.Action.UPDATE)
    }

    @PreRemove
    fun onEntityRemove(entity: Any) {
        if (entity !is ShikkEntity || (!entity::class.hasAnnotation<Tracing>() || entity::class.findAnnotation<Tracing>()?.delete == false))
            return

        traceActionService.createTraceAction(entity, TraceAction.Action.DELETE)
    }
}