package fr.shikkanime.controllers.site

import com.google.inject.Inject
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.services.caches.AnimeCacheService
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.services.caches.SimulcastCacheService
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import io.ktor.http.*
import java.time.ZonedDateTime

@Controller("/")
class SEOController {
    @Inject
    private lateinit var animeCacheService: AnimeCacheService

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
        val simulcasts = simulcastCacheService.findAll()!!.toMutableList()
        val animes = animeCacheService.findAll()!!
            .sortedBy { it.shortName.lowercase() }

        simulcasts.forEach { simulcast ->
            simulcast.lastReleaseDateTime =
                animes.filter { anime -> anime.simulcasts!!.size == 1 && anime.simulcasts.any { it.uuid == simulcast.uuid } }
                    .maxByOrNull { d -> ZonedDateTime.parse(d.releaseDateTime) }?.releaseDateTime
        }

        simulcasts.removeIf { simulcast -> simulcast.lastReleaseDateTime == null }

        val episodeMapping = episodeMappingCacheService.findAllBy(
            CountryCode.FR,
            null,
            null,
            listOf(SortParameter("lastReleaseDateTime", SortParameter.Order.DESC)),
            1,
            1
        )!!.data.firstOrNull()

        return Response.template(
            "/site/seo/sitemap.ftl",
            null,
            mutableMapOf(
                "episodeMapping" to episodeMapping,
                "simulcasts" to simulcasts,
                "animes" to animes,
                "seoLinks" to Link.entries.filter { !it.href.startsWith("/admin") && it.footer }.toList()
            ),
            contentType = ContentType.Text.Xml
        )
    }

    @Path("c97385827d194199b3a0509ec9221517.txt")
    @Get
    private fun indexNow(): Response {
        return Response.ok("c97385827d194199b3a0509ec9221517", contentType = ContentType.Text.Plain)
    }
}