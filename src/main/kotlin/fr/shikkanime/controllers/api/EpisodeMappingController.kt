package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.EpisodeMappingDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Delete
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Put
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

@Controller("/api/v1/episode-mappings")
class EpisodeMappingController : HasPageableRoute() {
    @Inject
    private lateinit var episodeMappingCacheService: EpisodeMappingCacheService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Path
    @Get
    @OpenAPI(
        "Get episode mappings",
        [
            OpenAPIResponse(
                200,
                "Episode mappings found",
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
        @QueryParam("status") statusParam: Status?,
    ): Response {
        val (page, limit, sortParameters) = pageableRoute(pageParam, limitParam, sortParam, descParam)

        return Response.ok(
            episodeMappingCacheService.findAllBy(
                CountryCode.fromNullable(countryParam) ?: CountryCode.FR,
                animeParam,
                sortParameters,
                page,
                limit,
                statusParam
            )
        )
    }

    @Path("/{uuid}")
    @Get
    @JWTAuthenticated
    @OpenAPI(hidden = true)
    private fun read(@PathParam("uuid") uuid: UUID): Response {
        return Response.ok(AbstractConverter.convert(episodeMappingService.find(uuid), EpisodeMappingDto::class.java))
    }

    @Path("/{uuid}")
    @Put
    @JWTAuthenticated
    @OpenAPI(hidden = true)
    private fun updateEpisode(
        @PathParam("uuid") uuid: UUID,
        @BodyParam episodeMappingDto: EpisodeMappingDto
    ): Response {
        val updated = episodeMappingService.update(uuid, episodeMappingDto)
        return Response.ok(AbstractConverter.convert(updated, EpisodeMappingDto::class.java))
    }

    @Path("/{uuid}")
    @Delete
    @JWTAuthenticated
    @OpenAPI(hidden = true)
    private fun deleteEpisode(@PathParam("uuid") uuid: UUID): Response {
        episodeMappingService.delete(episodeMappingService.find(uuid) ?: return Response.notFound())
        return Response.noContent()
    }

    @Path("/{uuid}/media-image")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun getMediaImage(@PathParam("uuid") uuid: UUID): Response {
        val image = ImageService.toEpisodeImage(
            AbstractConverter.convert(
                episodeVariantService.find(uuid),
                EpisodeVariantDto::class.java
            )
        )

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val bytes = baos.toByteArray()
        return Response.multipart(bytes, ContentType.Image.PNG)
    }
}