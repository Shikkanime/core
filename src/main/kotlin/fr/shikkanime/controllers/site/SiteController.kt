package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get

@Controller("/")
class SiteController {
    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeService: EpisodeService

    @Path
    @Get
    private fun home(): Response {
        return Response.template(
            Link.HOME,
            mutableMapOf(
                "animes" to AbstractConverter.convert(
                    animeService.findAllBy(
                        CountryCode.FR,
                        null,
                        listOf(SortParameter("releaseDateTime", SortParameter.Order.DESC)),
                        1,
                        6
                    ).data, AnimeDto::class.java
                ),
                "episodes" to AbstractConverter.convert(
                    episodeService.findAllBy(
                        CountryCode.FR,
                        null,
                        listOf(SortParameter("releaseDateTime", SortParameter.Order.DESC)),
                        1,
                        6
                    ).data, EpisodeDto::class.java
                )
            )
        )
    }
}