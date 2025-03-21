package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.animes.MissedAnimeDto
import fr.shikkanime.entities.miscellaneous.MissedAnime
import fr.shikkanime.factories.IGenericFactory

class MissedAnimeFactory : IGenericFactory<MissedAnime, MissedAnimeDto> {
    @Inject
    private lateinit var animeFactory: AnimeFactory

    override fun toDto(entity: MissedAnime) = MissedAnimeDto(
        anime = animeFactory.toDto(entity.anime),
        episodeMissed = entity.episodeMissed,
    )
}