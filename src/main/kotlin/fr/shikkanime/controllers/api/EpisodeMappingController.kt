package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.EpisodeMappingService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.services.caches.MemberFollowEpisodeCacheService
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

    @Inject
    private lateinit var memberFollowEpisodeCacheService: MemberFollowEpisodeCacheService

    @Path
    @Get
    @JWTAuthenticated(optional = true)
    @OpenAPI(
        "Get episode mappings",
        [
            OpenAPIResponse(
                200,
                "Episode mappings found",
                PageableDto::class,
            ),
            OpenAPIResponse(401, "Unauthorized")
        ],
        security = true
    )
    private fun getAll(
        @JWTUser
        memberUuid: UUID?,
        @QueryParam("country", description = "Country code to filter by", example = "FR", type = CountryCode::class)
        countryParam: CountryCode?,
        @QueryParam("anime", description = "UUID of the anime to filter by")
        animeParam: UUID?,
        @QueryParam("season", description = "Season number to filter by")
        seasonParam: Int?,
        @QueryParam("page", description = "Page number for pagination")
        pageParam: Int?,
        @QueryParam("limit", description = "Number of items per page. Must be between 1 and 30", example = "15")
        limitParam: Int?,
        @QueryParam(
            "sort",
            description = "Comma separated list of fields\n" +
                    "\n" +
                    "Possible values:\n" +
                    "- episodeType\n" +
                    "- releaseDateTime\n" +
                    "- lastReleaseDateTime\n" +
                    "- season\n" +
                    "- number\n" +
                    "- animeName",
            example = "lastReleaseDateTime,animeName,season,episodeType,number"
        )
        sortParam: String?,
        @QueryParam(
            "desc",
            description = "A comma-separated list of fields to sort in descending order",
            example = "lastReleaseDateTime,animeName,season,episodeType,number"
        )
        descParam: String?,
        @QueryParam("status", description = "Status to filter by", type = Status::class)
        statusParam: Status?,
    ): Response {
        val (page, limit, sortParameters) = pageableRoute(pageParam, limitParam, sortParam, descParam)

        if (memberUuid != null) {
            return Response.ok(
                memberFollowEpisodeCacheService.findAllBy(
                    memberUuid,
                    page,
                    limit
                )
            )
        }

        return Response.ok(
            episodeMappingCacheService.findAllBy(
                countryParam ?: CountryCode.FR,
                animeParam,
                seasonParam,
                sortParameters,
                page,
                limit,
                statusParam
            )
        )
    }

    @Path("/{uuid}")
    @Get
    @AdminSessionAuthenticated
    @OpenAPI(hidden = true)
    private fun read(@PathParam("uuid") uuid: UUID): Response {
        val find = episodeMappingService.find(uuid) ?: return Response.notFound()
        return Response.ok(AbstractConverter.convert(find, EpisodeMappingDto::class.java))
    }

    @Path("/{uuid}")
    @Put
    @AdminSessionAuthenticated
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
    @AdminSessionAuthenticated
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
        ImageIO.write(image, "jpg", baos)
        val bytes = baos.toByteArray()
        return Response.multipart(bytes, ContentType.Image.JPEG)
    }
}