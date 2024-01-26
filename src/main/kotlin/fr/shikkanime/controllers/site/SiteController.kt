package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeCacheService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get

@Controller("/")
class SiteController {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

    @Inject
    private lateinit var episodeCacheService: EpisodeCacheService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Path
    @Get
    private fun home(): Response {
        return Response.template(
            Link.HOME,
            mutableMapOf(
                "description" to configCacheService.getValueAsString(ConfigPropertyKey.SEO_DESCRIPTION),
                "animes" to animeCacheService.findAllBy(
                    CountryCode.FR,
                    null,
                    listOf(SortParameter("name", SortParameter.Order.ASC)),
                    1,
                    6
                )!!.data,
                "episodes" to episodeCacheService.findAllBy(
                    CountryCode.FR,
                    null,
                    listOf(SortParameter("releaseDateTime", SortParameter.Order.DESC)),
                    1,
                    6
                )!!.data
            )
        )
    }
}