package fr.shikkanime.services

import fr.shikkanime.entities.AnimeTag
import fr.shikkanime.repositories.AnimeTagRepository
import java.util.*

class AnimeTagService : AbstractService<AnimeTag, AnimeTagRepository>() {
    fun findAllByAnime(animeUuid: UUID) = repository.findAllByAnime(animeUuid)
}