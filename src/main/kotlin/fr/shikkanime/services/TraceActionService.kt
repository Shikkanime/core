package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.TraceActionRepository
import java.time.ZonedDateTime

class TraceActionService : AbstractService<TraceAction, TraceActionRepository>() {
    @Inject
    private lateinit var traceActionRepository: TraceActionRepository

    override fun getRepository() = traceActionRepository

    fun findAllBy(page: Int, limit: Int) = traceActionRepository.findAllBy(page, limit)

    fun createTraceAction(shikkEntity: ShikkEntity, action: TraceAction.Action) = save(
        TraceAction(
            actionDateTime = ZonedDateTime.now(),
            entityType = shikkEntity::class.java.simpleName,
            entityUuid = shikkEntity.uuid,
            action = action
        )
    )
}