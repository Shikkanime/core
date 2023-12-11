package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.repositories.AnimeRepository

class AnimeService : AbstractService<Anime, AnimeRepository>() {
    @Inject
    private lateinit var animeRepository: AnimeRepository

    override fun getRepository(): AnimeRepository {
        return animeRepository
    }

    fun findByName(countryCode: CountryCode, name: String?): Anime? {
        return animeRepository.findByName(countryCode, name)
    }
}