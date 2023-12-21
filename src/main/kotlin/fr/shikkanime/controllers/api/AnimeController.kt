package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.SortParameter
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.ImageService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.openapi.OpenAPI
import fr.shikkanime.utils.routes.openapi.OpenAPIResponse
import fr.shikkanime.utils.routes.param.PathParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import java.util.*

@Controller("/api/v1/animes")
class AnimeController {
    @Inject
    private lateinit var animeService: AnimeService

    @Path
    @Get
    @OpenAPI(
        "Get animes",
        [
            OpenAPIResponse(
                200,
                "Animes found",
                Array<AnimeDto>::class,
            ),
            OpenAPIResponse(
                409,
                "You can't use simulcast and name at the same time OR You can't use sort and desc with name",
                MessageDto::class
            ),
        ]
    )
    private fun getAll(
        @QueryParam("name") name: String?,
        @QueryParam("country") countryParam: CountryCode?,
        @QueryParam("simulcast") simulcastParam: UUID?,
        @QueryParam("page") pageParam: Int?,
        @QueryParam("limit") limitParam: Int?,
        @QueryParam("sort") sortParam: String?,
        @QueryParam("desc") descParam: String?,
    ): Response {
        if (simulcastParam != null && name != null) {
            return Response.conflict(
                MessageDto(
                    MessageDto.Type.ERROR,
                    "You can't use simulcast and name at the same time",
                )
            )
        }

        if (name != null && (sortParam != null || descParam != null)) {
            return Response.conflict(
                MessageDto(
                    MessageDto.Type.ERROR,
                    "You can't use sort and desc with name",
                )
            )
        }

        val country = countryParam ?: CountryCode.FR
        val page = pageParam ?: 1
        var limit = limitParam ?: 15
        if (limit < 1) limit = 1
        if (limit > 30) limit = 30

        val sortParameters = mutableListOf<SortParameter>()

        if (!sortParam.isNullOrBlank()) {
            val sortParams = sortParam.split(",")
            val descParams = descParam?.split(",") ?: listOf()

            sortParams.forEach { sort ->
                val desc = descParams.contains(sort)
                sortParameters.add(SortParameter(sort, if (desc) SortParameter.Order.DESC else SortParameter.Order.ASC))
            }
        }

        val list = if (!name.isNullOrBlank()) {
            animeService.findByName(name, country, page, limit)
        } else if (simulcastParam != null) {
            animeService.findBySimulcast(simulcastParam, country, sortParameters, page, limit)
        } else {
            animeService.findAll(sortParameters, page, limit)
        }

        return Response.ok(AbstractConverter.convert(list, AnimeDto::class.java))
    }

    @Path("/{uuid}")
    @Get
    @OpenAPI(
        "Get anime",
        [
            OpenAPIResponse(
                200,
                "Anime found",
                AnimeDto::class,
            ),
            OpenAPIResponse(
                404,
                "Anime not found",
                MessageDto::class,
            ),
        ]
    )
    private fun getAnime(@PathParam("uuid") uuid: UUID): Response {
        val anime = animeService.find(uuid) ?: return Response.notFound(MessageDto(MessageDto.Type.ERROR, "Anime not found"))
        return Response.ok(AbstractConverter.convert(anime, AnimeDto::class.java))
    }

    @Path("/{uuid}/image")
    @Get
    @Cached(maxAgeSeconds = 3600)
    @OpenAPI(
        "Get anime image",
        [
            OpenAPIResponse(
                200,
                "Image found",
                ByteArray::class,
                "image/webp"
            ),
            OpenAPIResponse(
                404,
                "Anime not found OR Anime image not found",
                MessageDto::class,
            ),
        ]
    )
    private fun getAnimeImage(@PathParam("uuid") uuid: UUID): Response {
        val anime = animeService.find(uuid) ?: return Response.notFound(MessageDto(MessageDto.Type.ERROR, "Anime not found"))
        val image = ImageService[anime.uuid!!] ?: return Response.notFound(MessageDto(MessageDto.Type.ERROR, "Anime image not found"))
        return Response.multipart(image.bytes, ContentType.parse("image/webp"))
    }
}