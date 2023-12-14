package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.utils.routes.Cached
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.ziedelth.utils.routes.method.Get
import io.ktor.http.*
import java.util.*

@Controller("/api/animes")
class AnimeController {
    @Inject
    private lateinit var animeService: AnimeService

    @Path("/image/{uuid}")
    @Get
    @Cached(maxAgeSeconds = 3600)
    private fun getImage(uuid: UUID): Response {
        val anime = animeService.find(uuid) ?: return Response.notFound("Anime not found")
        val image = ImageService[anime.uuid!!] ?: return Response.notFound("Image not found")
        return Response.multipart(image.bytes, ContentType.parse("image/webp"))
    }

    @Path("/search/{countryCode}/{name}")
    @Get
    private fun searchByCountryCodeAndName(countryCode: CountryCode?, name: String): Response {
        if (countryCode == null) {
            return Response.badRequest("Country code is null")
        }

        return Response.ok(AbstractConverter.convert(animeService.findByName(countryCode, name), AnimeDto::class.java))
    }
}