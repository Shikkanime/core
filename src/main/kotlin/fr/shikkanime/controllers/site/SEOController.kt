package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.controllers.admin.ADMIN
import fr.shikkanime.dtos.URLDto
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.withUTCString
import io.ktor.http.*
import java.time.ZonedDateTime

@Controller("/")
class SEOController {
    private fun ZonedDateTime.formatDateTime() = this.withUTCString().replace("Z", "+00:00")

    @Inject
    private lateinit var episodeMappingCacheService: EpisodeMappingCacheService

    @Inject
    private lateinit var simulcastCacheService: SimulcastCacheService

    @Path("robots.txt")
    @Get
    private fun robots(): Response {
        return Response.template(
            "/site/seo/robots.ftl",
            null,
            contentType = ContentType.Text.Plain
        )
    }

    @Path("sitemap.xml")
    @Get
    private fun sitemap(): Response {
        val globalLastModification = "2024-03-20T17:00:00+00:00"

        val lastReleaseDateTime = episodeMappingCacheService.findAllBy(
            CountryCode.FR,
            null,
            null,
            listOf(SortParameter("lastReleaseDateTime", SortParameter.Order.DESC)),
            1,
            1
        ).data.firstOrNull()?.lastReleaseDateTime?.replace("Z", "+00:00") ?: globalLastModification

        val urls = mutableSetOf(
            URLDto(Constant.baseUrl, lastReleaseDateTime),
            URLDto("${Constant.baseUrl}/calendar", lastReleaseDateTime),
            URLDto("${Constant.baseUrl}/search", globalLastModification)
        )

        simulcastCacheService.findAll().mapTo(urls) {
            URLDto("${Constant.baseUrl}/catalog/${it.slug}", it.lastReleaseDateTime!!)
        }

        episodeMappingCacheService.findAllSeo()
            .groupBy { it.animeSlug }
            .forEach { (animeSlug, episodes) ->
                val seasonMap = episodes.groupBy { it.season }
                val firstSeasonDateTime = seasonMap.values.flatten().maxOf { it.lastReleaseDateTime }

                urls.add(URLDto("${Constant.baseUrl}/animes/$animeSlug", firstSeasonDateTime.formatDateTime()))

                seasonMap.forEach { (season, seasonEpisodes) ->
                    val lastSeasonDateTime = seasonEpisodes.maxOf { it.lastReleaseDateTime }
                    urls.add(URLDto("${Constant.baseUrl}/animes/$animeSlug/season-$season", lastSeasonDateTime.formatDateTime()))

                    seasonEpisodes.forEach {
                        urls.add(URLDto("${Constant.baseUrl}/animes/$animeSlug/season-$season/${it.episodeType.slug}-${it.number}", it.lastReleaseDateTime.formatDateTime()))
                    }
                }
            }

        Link.entries.filter { !it.href.startsWith(ADMIN) && it.footer }
            .mapTo(urls) { URLDto("${Constant.baseUrl}${it.href}", globalLastModification) }

        return Response.template(
            "/site/seo/sitemap.ftl",
            null,
            mutableMapOf("urls" to urls),
            contentType = ContentType.Text.Xml
        )
    }

    @Path("c97385827d194199b3a0509ec9221517.txt")
    @Get
    private fun indexNow(): Response {
        return Response.ok("c97385827d194199b3a0509ec9221517", contentType = ContentType.Text.Plain)
    }

    @Path("/feed/episodes")
    @Get
    private fun feedRss(): Response {
        val data = episodeMappingCacheService.findAllGroupedBy(
            CountryCode.FR,
            1,
            50
        ).data

        return Response.template(
            "/site/seo/rss.ftl",
            null,
            mutableMapOf("groupedEpisodes" to data),
            contentType = ContentType.Text.Xml
        )
    }
}