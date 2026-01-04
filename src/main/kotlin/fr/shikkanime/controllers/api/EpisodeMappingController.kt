package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.MediaImage
import fr.shikkanime.services.MediaVideo
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.services.caches.MemberFollowEpisodeCacheService
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.http.*
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

@Controller("/api/v1/episode-mappings")
class EpisodeMappingController : HasPageableRoute() {
    @Inject private lateinit var episodeMappingCacheService: EpisodeMappingCacheService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var memberFollowEpisodeCacheService: MemberFollowEpisodeCacheService

    @Path
    @Get
    @JWTAuthenticated(optional = true)
    private fun getAll(
        @JWTUser memberUuid: UUID?,
        @QueryParam parameters: Map<String, String>
    ): Response {
        val countryCode = CountryCode.fromNullable(parameters["country"])
        val animeUuid = parameters["anime"]?.let(UUID::fromString)
        val season = parameters["season"]?.toIntOrNull()
        val sort = parameters["sort"]
        val desc = parameters["desc"]

        val (page, limit, sortParameters) = pageableRoute(
            parameters["page"]?.toIntOrNull(),
            parameters["limit"]?.toIntOrNull() ?: 9,
            sort,
            desc
        )

        return Response.ok(
            if (memberUuid != null) {
                memberFollowEpisodeCacheService.findAllBy(memberUuid, page, limit)
            } else {
                episodeMappingCacheService.findAllBy(countryCode, animeUuid, season, sortParameters, page, limit)
            }
        )
    }

    @Path("/media-image")
    @Get
    private fun getMediaImage(@QueryParam("uuids") uuidsGzip: String?): Response {
        if (uuidsGzip.isNullOrBlank()) return Response.badRequest()

        val fromGzip = runCatching { EncryptionManager.fromGzip(uuidsGzip) }.getOrNull() ?: return Response.badRequest()
        val uuids = fromGzip.split(StringUtils.COMMA_STRING).mapNotNull(UUID::fromString).distinct()
        val variants = episodeVariantService.findAllByUuids(uuids)
        if (variants.isEmpty()) return Response.notFound()

        val distinctAnimeUuids = variants.map { it.mapping!!.anime!!.uuid }.distinct()
        if (distinctAnimeUuids.size != 1) return Response.badRequest()

        val image = MediaImage.toMediaImage(*variants.toTypedArray())
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", baos)
        return Response.multipart(baos.toByteArray(), ContentType.Image.JPEG)
    }

    @Path("/media-video")
    @Get
    private fun getMediaVideo(@QueryParam("uuids") uuidsGzip: String?): Response {
        if (uuidsGzip.isNullOrBlank()) return Response.badRequest()

        val fromGzip = runCatching { EncryptionManager.fromGzip(uuidsGzip) }.getOrNull() ?: return Response.badRequest()
        val uuids = fromGzip.split(StringUtils.COMMA_STRING).mapNotNull(UUID::fromString).distinct()
        val variants = episodeVariantService.findAllByUuids(uuids)
        if (variants.isEmpty()) return Response.notFound()

        val distinctAnimeUuids = variants.map { it.mapping!!.anime!!.uuid }.distinct()
        if (distinctAnimeUuids.size != 1) return Response.badRequest()

        val video = MediaVideo.toMediaVideo(*variants.toTypedArray())
        return Response.multipart(video.readBytes(), ContentType.Video.MP4)
    }
}