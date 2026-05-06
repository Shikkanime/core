package fr.shikkanime.services

import fr.shikkanime.entities.Genre
import fr.shikkanime.repositories.GenreRepository
import java.util.*

class GenreService : AbstractService<Genre, GenreRepository>() {
    suspend fun findAllByAnime(animeUuid: UUID) = repository.findAllByAnime(animeUuid)

    suspend fun findByName(name: String) = repository.findByName(name)

    suspend fun findOrSave(name: String) = repository.findByName(name) ?: save(Genre(name = name))
}