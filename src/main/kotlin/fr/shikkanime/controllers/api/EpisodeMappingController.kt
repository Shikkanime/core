package fr.shikkanime.controllers.api

import com.google.inject.Inject
import fr.shikkanime.dtos.MessageDto
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.factories.impl.GroupedEpisodeFactory
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.MediaImage
import fr.shikkanime.services.caches.EpisodeMappingCacheService
import fr.shikkanime.services.caches.MemberFollowEpisodeCacheService
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.ifNullOrBlank
import fr.shikkanime.utils.routes.*
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.param.QueryParam
import fr.shikkanime.utils.takeIfNotEmpty
import fr.shikkanime.utils.toByteArray
import io.ktor.http.*
import java.util.*

@Controller("/api/v1/episode-mappings")
class EpisodeMappingController : HasPageableRoute() {
    @Inject private lateinit var episodeMappingCacheService: EpisodeMappingCacheService
    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var memberFollowEpisodeCacheService: MemberFollowEpisodeCacheService
    @Inject private lateinit var groupedEpisodeFactory: GroupedEpisodeFactory

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
        val searchTypes =
            parameters["searchTypes"]?.split(StringUtils.COMMA_STRING)?.mapNotNull(LangType::valueOfNullable)
                ?.toTypedArray()

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
                episodeMappingCacheService.findAllBy(
                    countryCode,
                    animeUuid,
                    season,
                    searchTypes,
                    sortParameters,
                    page,
                    limit
                )
            }
        )
    }

    @Path("/media-image")
    @Get
    private suspend fun getMediaImage(@QueryParam("uuids") uuidsGzip: String?): Response {
        val uuids = uuidsGzip.ifNullOrBlank { return Response.badRequest(MessageDto.error("Missing 'uuids' query parameter")) }
            .runCatching(EncryptionManager::fromGzip).getOrElse { return Response.badRequest(MessageDto.error("Failed to decode 'uuids' parameter (expected gzip-encoded data)")) }
            .split(StringUtils.COMMA_STRING)
            .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
            .distinct()
            .takeIfNotEmpty() ?: return Response.badRequest(MessageDto.error("No valid UUIDs found in 'uuids' parameter"))

        val groupedEpisodes = episodeVariantService.findAllByUuids(uuids)
            .takeIfNotEmpty()
            ?.let(groupedEpisodeFactory::toEntities) ?: return Response.notFound(MessageDto.error("No episode variant found for the provided UUIDs"))

        return Response.multipart(
            MediaImage.toMediaImage(groupedEpisodes).toByteArray(),
            ContentType.Image.JPEG
        )
    }
}