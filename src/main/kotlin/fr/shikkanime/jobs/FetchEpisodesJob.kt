package fr.shikkanime.jobs

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.ImageService
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

        if (!isInitialized) {
            val variants = episodeVariantService.findAll()
            identifiers.addAll(variants.mapNotNull { it.identifier }.toSet())

            // COMPARE COUNTRY, ANIME, PLATFORM, EPISODE TYPE, SEASON, NUMBER, AND LANG TYPE
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
            .sortedWith(compareBy({ it.releaseDateTime }, { it.anime }, { it.season }, { it.episodeType }, { it.number }, { it.audioLocale }))
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

        sendToNetworks(savedEpisodes)
        isRunning = false
    }

    private fun sendToNetworks(savedEpisodes: List<EpisodeVariant>) {
        if (savedEpisodes.isNotEmpty() && savedEpisodes.size < configCacheService.getValueAsInt(ConfigPropertyKey.SOCIAL_NETWORK_EPISODES_SIZE_LIMIT)) {
            for (episode in savedEpisodes) {
                val typeIdentifier = getTypeIdentifier(episode)

                if (typeIdentifiers.contains(typeIdentifier)) {
                    continue
                }

                typeIdentifiers.add(typeIdentifier)
                val episodeDto = AbstractConverter.convert(episode, EpisodeVariantDto::class.java)
                sendToSocialNetworks(episodeDto)
                FirebaseNotification.send(episodeDto)
            }
        }
    }

    private fun getTypeIdentifier(episodeVariant: EpisodeVariant): String {
        val country = episodeVariant.mapping!!.anime!!.countryCode!!
        val anime = episodeVariant.mapping!!.anime!!.uuid!!
        val platform = episodeVariant.platform!!
        val episodeType = episodeVariant.mapping!!.episodeType!!
        val season = episodeVariant.mapping!!.season!!
        val number = episodeVariant.mapping!!.number!!
        val audioLocale = episodeVariant.audioLocale!!

        val langType = LangType.fromAudioLocale(country, audioLocale)
        val typeIdentifier = "${country}_${anime}_${platform}_${episodeType}_${season}_${number}_${langType}"
        return typeIdentifier
    }

    private fun sendToSocialNetworks(dto: EpisodeVariantDto) {
        val mediaImage = try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(ImageService.toEpisodeImage(dto), "png", byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while converting episode image for social networks", e)
            return
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