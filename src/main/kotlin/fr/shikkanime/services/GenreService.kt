package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Genre
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.GenreRepository
import java.util.*

class GenreService : AbstractService<Genre, GenreRepository>() {
    @Inject private lateinit var traceActionService: TraceActionService

    fun findAllByAnime(animeUuid: UUID) = repository.findAllByAnime(animeUuid)

    fun findByName(name: String) = repository.findByName(name)

    fun findOrSave(name: String) = repository.findByName(name) ?: save(Genre(name = name))

    override fun save(entity: Genre): Genre {
        val genre = super.save(entity)
        traceActionService.createTraceAction(genre, TraceAction.Action.CREATE)
        return genre
    }
}