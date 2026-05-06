package fr.shikkanime.modules

import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.services.TraceActionService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.entities.Tracing
import jakarta.persistence.PrePersist
import jakarta.persistence.PreRemove
import jakarta.persistence.PreUpdate
import kotlinx.coroutines.runBlocking
import kotlin.reflect.full.findAnnotation

class EntityLifecycleListener {
    private val traceActionService: TraceActionService by lazy {
        Constant.injector.getInstance(TraceActionService::class.java)
    }

    private fun trace(
        entity: Any,
        action: TraceAction.Action,
        requireDeleteEnabled: Boolean = false
    ) {
        if (entity !is ShikkEntity) return

        val tracing = entity::class.findAnnotation<Tracing>() ?: return
        if (requireDeleteEnabled && !tracing.delete) return

        runBlocking {
            traceActionService.createTraceAction(entity, action)
        }
    }

    @PrePersist
    fun onEntityCreate(entity: Any) {
        trace(entity, TraceAction.Action.CREATE)
    }

    @PreUpdate
    fun onEntityUpdate(entity: Any) {
        trace(entity, TraceAction.Action.UPDATE)
    }

    @PreRemove
    fun onEntityRemove(entity: Any) {
        trace(entity, TraceAction.Action.DELETE, requireDeleteEnabled = true)
    }
}