package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.caches.EpisodeCacheService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO


@Controller("/api/v1/episodes")
class EpisodeController : HasPageableRoute() {
    @Inject
    private lateinit var episodeService: EpisodeService

    @Inject
    private lateinit var episodeCacheService: EpisodeCacheService

    @Path
    @Get
    @OpenAPI(
        "Get episodes",
        [
            OpenAPIResponse(
                200,
                "Episodes found",
                PageableDto::class,
            ),
        ]
    )
    private fun getAll(
        @QueryParam("country", description = "By default: FR", type = CountryCode::class) countryParam: String?,
        @QueryParam("anime") animeParam: UUID?,
        @QueryParam("page") pageParam: Int?,
        @QueryParam("limit") limitParam: Int?,
        @QueryParam("sort") sortParam: String?,
        @QueryParam("desc") descParam: String?,
    ): Response {
        val (page, limit, sortParameters) = pageableRoute(pageParam, limitParam, sortParam, descParam)
        return Response.ok(
            episodeCacheService.findAllBy(
                CountryCode.fromNullable(countryParam) ?: CountryCode.FR,
                animeParam,
                sortParameters,
                page,
                limit
            )
        )
    }

    @Path("/{uuid}/media-image")
    @Get
    @Cached(maxAgeSeconds = 3600)
    @OpenAPI(hidden = true)
    @AdminSessionAuthenticated
    private fun getEpisodeMediaImage(@PathParam("uuid") uuid: UUID): Response {
        val episode =
            episodeService.find(uuid) ?: return Response.notFound(
                MessageDto(
                    MessageDto.Type.ERROR,
                    "Episode not found"
                )
            )

        val image = ImageService.toEpisodeImage(AbstractConverter.convert(episode, EpisodeDto::class.java))
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val bytes = baos.toByteArray()
        return Response.multipart(bytes, ContentType.Image.PNG)
    }
}