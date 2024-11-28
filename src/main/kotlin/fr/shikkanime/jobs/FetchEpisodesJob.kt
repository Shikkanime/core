package fr.shikkanime.jobs

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.dtos.variants.EpisodeVariantIdentifierDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.*
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.MediaImage
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import jakarta.inject.Inject
import java.io.ByteArrayOutputStream
import java.time.ZonedDateTime
import java.util.logging.Level
import javax.imageio.ImageIO

class FetchEpisodesJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var isInitialized = false
    private var isRunning = false
    private var lock = 0
    private val maxLock = 5

    private val identifiers = mutableSetOf<String>()
    private val typeIdentifiers = mutableSetOf<String>()

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun run() {
        if (isRunning) {
            if (++lock > maxLock) {
                logger.warning("Job is locked, unlocking...")
                isRunning = false
                lock = 0
            } else {
                logger.warning("Job is already running ($lock/$maxLock)")
                return
            }
        }

        isRunning = true
        lock = 0

        if (!isInitialized) {
            val variants = episodeVariantService.findAllTypeIdentifier()
            identifiers.addAll(variants.map { it.identifier })

            Constant.abstractPlatforms.forEach {
                it.hashCache.addAll(
                    variants.filter { variant -> variant.platform == it.getPlatform() }
                        .map { variant ->
                            ".{2}-.{4}-(.*)-.{2}-.{2}".toRegex().find(variant.identifier)!!.groupValues[1]
                        }
                )
            }

            variants.forEach { typeIdentifiers.add(getTypeIdentifier(it)) }
            isInitialized = true
        }

        val zonedDateTime = ZonedDateTime.now().withSecond(0).withNano(0).withUTC()
        val episodes = mutableListOf<AbstractPlatform.Episode>()

        Constant.abstractPlatforms.forEach { abstractPlatform ->
            logger.info("Fetching episodes for ${abstractPlatform.getPlatform().name}...")

            try {
                episodes.addAll(abstractPlatform.fetchEpisodes(zonedDateTime))
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Error while fetching episodes for ${abstractPlatform.getPlatform().name}", e)
            }
        }

        val savedEpisodes = episodes
            .sortedWith(
                compareBy(
                    { it.releaseDateTime },
                    { it.anime },
                    { it.season },
                    { it.episodeType },
                    { it.number },
                    { LangType.fromAudioLocale(it.countryCode, it.audioLocale) })
            )
            .filter { (zonedDateTime.isEqualOrAfter(it.releaseDateTime)) && !identifiers.contains(it.getIdentifier()) }
            .mapNotNull {
                try {
                    val savedEpisode = episodeVariantService.save(it)
                    identifiers.add(it.getIdentifier())
                    savedEpisode
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error while saving episode ${it.getIdentifier()} (${it.anime})", e)
                    null
                }
            }

        isRunning = false

        if (savedEpisodes.isEmpty()) return
        MapCache.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java)
        sendToNetworks(savedEpisodes)
    }

    private fun getTypeIdentifier(input: Any): String {
        val episodeVariantIdentifierDto = when (input) {
            is EpisodeVariantIdentifierDto -> input
            is EpisodeVariant -> EpisodeVariantIdentifierDto(
                input.mapping!!.anime!!.countryCode!!,
                input.mapping!!.anime!!.uuid!!,
                input.platform!!,
                input.mapping!!.season!!,
                input.mapping!!.episodeType!!,
                input.mapping!!.number!!,
                input.audioLocale!!,
                input.identifier!!
            )

            else -> throw IllegalArgumentException("Invalid input type")
        }

        val langType = LangType.fromAudioLocale(episodeVariantIdentifierDto.countryCode, episodeVariantIdentifierDto.audioLocale)
        return "${episodeVariantIdentifierDto.countryCode}_${episodeVariantIdentifierDto.animeUuid}_${episodeVariantIdentifierDto.season}_${episodeVariantIdentifierDto.episodeType}_${episodeVariantIdentifierDto.number}_${langType}"
    }

    private fun sendToNetworks(savedEpisodes: List<EpisodeVariant>) {
        val sizeLimit = configCacheService.getValueAsInt(ConfigPropertyKey.SOCIAL_NETWORK_EPISODES_SIZE_LIMIT)

        savedEpisodes
            .groupBy { it.mapping?.anime?.uuid }
            .values
            .forEach { episodes ->
                episodes
                    .filterNot { typeIdentifiers.contains(getTypeIdentifier(it)) }
                    .takeIf { it.size < sizeLimit }
                    ?.forEach { episode ->
                        val typeIdentifier = getTypeIdentifier(episode)

                        if (typeIdentifiers.add(typeIdentifier)) {
                            val episodeDto = AbstractConverter.convert(episode, EpisodeVariantDto::class.java)
                            sendEpisodeNotification(episodeDto)
                            sendToSocialNetworks(episodeDto)
                        }
                    }
            }
    }

    private fun sendEpisodeNotification(episodeDto: EpisodeVariantDto) {
        try {
            FirebaseNotification.send(episodeDto)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while sending notification for episode ${episodeDto.identifier}", e)
        }
    }

    private fun sendToSocialNetworks(dto: EpisodeVariantDto) {
        val mediaImage = try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(MediaImage.toMediaImage(dto), "jpg", byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while converting episode image for social networks", e)
            null
        }

        Constant.abstractSocialNetworks.parallelStream().forEach { socialNetwork ->
            try {
                socialNetwork.sendEpisodeRelease(dto, mediaImage)
            } catch (e: Exception) {
                logger.log(
                    Level.SEVERE,
                    "Error while sending episode release for ${
                        socialNetwork.javaClass.simpleName.replace(
                            "SocialNetwork",
                            ""
                        )
                    }",
                    e
                )
            }
        }
    }
}