package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.AnimeTag
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.AnimeTagRepository
import java.util.*

class AnimeTagService : AbstractService<AnimeTag, AnimeTagRepository>() {
    @Inject private lateinit var traceActionService: TraceActionService

    fun findAllByAnime(animeUuid: UUID) = repository.findAllByAnime(animeUuid)

    override fun update(entity: AnimeTag): AnimeTag {
        val tag = super.update(entity)
        traceActionService.createTraceAction(tag, TraceAction.Action.UPDATE)
        return tag
    }

    override fun saveAll(entities: List<AnimeTag>) {
        super.saveAll(entities)
        traceActionService.createTraceActions(entities, TraceAction.Action.CREATE)
    }

    override fun deleteAll(entities: List<AnimeTag>) {
        super.deleteAll(entities)
        traceActionService.createTraceActions(entities, TraceAction.Action.DELETE)
    }
}