package fr.shikkanime.services

import fr.shikkanime.entities.AnimeTag
import fr.shikkanime.repositories.AnimeTagRepository
import java.util.*

class AnimeTagService : AbstractService<AnimeTag, AnimeTagRepository>() {
    suspend fun findAllByAnime(animeUuid: UUID) = repository.findAllByAnime(animeUuid)
}