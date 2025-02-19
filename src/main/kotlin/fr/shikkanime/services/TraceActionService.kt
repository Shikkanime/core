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

    fun findAllBy(entityType: String?, action: String?, page: Int, limit: Int) = traceActionRepository.findAllBy(entityType, action, page, limit)

    fun getLoginCountsAfter(date: ZonedDateTime) = traceActionRepository.getLoginCountsAfter(date)

    fun createTraceAction(shikkEntity: ShikkEntity, action: TraceAction.Action) = save(
        TraceAction(
            actionDateTime = ZonedDateTime.now(),
            entityType = shikkEntity::class.java.simpleName,
            entityUuid = shikkEntity.uuid,
            action = action
        )
    )
}