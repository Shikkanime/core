package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.factories.impl.EpisodeVariantFactory
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.MediaImage
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.services.caches.MemberFollowEpisodeCacheService
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
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
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var memberFollowEpisodeCacheService: MemberFollowEpisodeCacheService

    @Inject
    private lateinit var episodeVariantFactory: EpisodeVariantFactory

    @Path
    @Get
    @JWTAuthenticated(optional = true)
    private fun getAll(
        @JWTUser
        memberUuid: UUID?,
        @QueryParam("country")
        countryParam: CountryCode?,
        @QueryParam("anime")
        animeParam: UUID?,
        @QueryParam("season")
        seasonParam: Int?,
        @QueryParam("page")
        pageParam: Int?,
        @QueryParam("limit")
        limitParam: Int?,
        @QueryParam("sort")
        sortParam: String?,
        @QueryParam("desc")
        descParam: String?,
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
                limit
            )
        )
    }

    @Path("/{uuid}/media-image")
    @Get
    private fun getMediaImage(
        @PathParam("uuid")
        uuid: UUID
    ): Response {
        val episodeVariant = episodeVariantService.find(uuid) ?: return Response.notFound()
        val image = MediaImage.toMediaImage(listOf(episodeVariantFactory.toDto(episodeVariant)))

        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", baos)
        val bytes = baos.toByteArray()
        return Response.multipart(bytes, ContentType.Image.JPEG)
    }
}