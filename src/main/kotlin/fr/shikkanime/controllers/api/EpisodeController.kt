package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.dtos.PageableDto
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.utils.routes.Cached
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import java.util.*

@Controller("/api/v1/episodes")
class EpisodeController {
    @Inject
    private lateinit var episodeService: EpisodeService

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
        @QueryParam("country") countryParam: CountryCode?,
        @QueryParam("anime") animeParam: UUID?,
        @QueryParam("page") pageParam: Int?,
        @QueryParam("limit") limitParam: Int?,
        @QueryParam("sort") sortParam: String?,
        @QueryParam("desc") descParam: String?,
    ): Response {
        val page = pageParam ?: 1
        val limit = limitParam?.coerceIn(1, 30) ?: 15

        val sortParameters = sortParam?.split(",")?.map { sort ->
            val desc = descParam?.split(",")?.contains(sort) ?: false
            SortParameter(sort, if (desc) SortParameter.Order.DESC else SortParameter.Order.ASC)
        } ?: mutableListOf()

        val pageable = episodeService.findAllBy(countryParam, animeParam, sortParameters, page, limit)
        return Response.ok(PageableDto.fromPageable(pageable, EpisodeDto::class.java))
    }

    @Path("/{uuid}/image")
    @Get
    @Cached(maxAgeSeconds = 3600)
    @OpenAPI(
        "Get episode image",
        [
            OpenAPIResponse(
                200,
                "Image found",
                ByteArray::class,
                "image/webp"
            ),
            OpenAPIResponse(
                404,
                "Episode not found OR Episode image not found",
                MessageDto::class,
            ),
        ]
    )
    private fun getEpisodeImage(@PathParam("uuid") uuid: UUID): Response {
        val episode =
            episodeService.find(uuid) ?: return Response.notFound(
                MessageDto(
                    MessageDto.Type.ERROR,
                    "Episode not found"
                )
            )

        val image = ImageService[episode.uuid!!] ?: return Response.notFound(
            MessageDto(
                MessageDto.Type.ERROR,
                "Episode image not found"
            )
        )
        return Response.multipart(image.bytes, ContentType.parse("image/webp"))
    }
}