package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.admin.EpisodeMappingAdminService
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import io.ktor.http.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.Level

@Controller("$ADMIN/variant-shifter")
@AdminSessionAuthenticated
class AdminVariantShifterController {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject private lateinit var episodeMappingAdminService: EpisodeMappingAdminService

    @Path
    @Get
    private fun getVariantShifter(): Response {
        val episodeTypes = EpisodeType.entries.toTypedArray()

        return Response.template(
            Link.VARIANT_SHIFTER,
            mutableMapOf(
                "episodeTypes" to episodeTypes
            )
        )
    }

    @Path
    @Post
    private fun postVariantShifter(@BodyParam parameters: Parameters): Response {
        try {
            val animeUuidStr = parameters["animeUuid"] ?: throw IllegalArgumentException("animeUuid is required")
            val platformStr = parameters["platform"] ?: throw IllegalArgumentException("platform is required")
            val episodeTypeStr = parameters["episodeType"] ?: throw IllegalArgumentException("episodeType is required")
            val seasonStr = parameters["season"] ?: throw IllegalArgumentException("season is required")
            val startNumberStr = parameters["startNumber"] ?: throw IllegalArgumentException("startNumber is required")
            val endNumberStr = parameters["endNumber"] ?: throw IllegalArgumentException("endNumber is required")
            val shiftStr = parameters["shift"] ?: throw IllegalArgumentException("shift is required")

            val animeUuid = UUID.fromString(animeUuidStr)
            val platform = Platform.valueOf(platformStr)
            val episodeType = EpisodeType.valueOf(episodeTypeStr)
            val season = seasonStr.toInt()
            val startNumber = startNumberStr.toInt()
            val endNumber = endNumberStr.toInt()
            val shift = shiftStr.toInt()

            require(endNumber >= startNumber) { "End number must be greater than or equal to start number" }

            val variantsMoved = episodeMappingAdminService.shiftVariants(
                animeUuid,
                platform,
                episodeType,
                season,
                startNumber,
                endNumber,
                shift
            )

            InvalidationService.invalidate(
                Anime::class.java,
                EpisodeMapping::class.java,
                EpisodeVariant::class.java,
                Simulcast::class.java
            )

            val successMessage =
                URLEncoder.encode("$variantsMoved variant(s) shifted successfully", StandardCharsets.UTF_8)
            return Response.redirect("${Link.VARIANT_SHIFTER.href}?success=$successMessage")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error shifting variants", e)
            val errorMessage = URLEncoder.encode(e.message ?: "An error occurred", StandardCharsets.UTF_8)
            return Response.redirect("${Link.VARIANT_SHIFTER.href}?error=$errorMessage")
        }
    }
}
