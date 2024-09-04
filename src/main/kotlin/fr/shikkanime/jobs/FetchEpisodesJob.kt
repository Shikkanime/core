package fr.shikkanime.jobs

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.*
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.ImageService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import jakarta.inject.Inject
import jakarta.persistence.Tuple
import java.io.ByteArrayOutputStream
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Level
import javax.imageio.ImageIO

class FetchEpisodesJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var isInitialized = false
    private var isRunning = false
    private var lock = 0
    private val maxLock = 5

    private val identifiers = mutableSetOf<String>()
    private val typeIdentifiersWithPlatforms = mutableSetOf<String>()
    private val typeIdentifiersWithoutPlatforms = mutableSetOf<String>()

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
            val elements = variants.map { it[7] as String }.toSet()
            identifiers.addAll(elements)

            Constant.abstractPlatforms.forEach {
                it.hashCache.addAll(
                    variants.filter { variant -> (variant[2] as Platform) == it.getPlatform() }
                        .map { variant ->
                            ".{2}-.{4}-(.*)-.{2}-.{2}".toRegex().find(variant[7] as String)!!.groupValues[1]
                        }
                )
            }

            // COMPARE COUNTRY, ANIME, PLATFORM, EPISODE TYPE, SEASON, NUMBER, AND LANG TYPE
            variants.forEach {
                typeIdentifiersWithPlatforms.add(getTypeIdentifierWithPlatform(it))
                typeIdentifiersWithoutPlatforms.add(getTypeIdentifierWithoutPlatform(it))
            }

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
                    { it.audioLocale })
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

        sendToNetworks(savedEpisodes)
        isRunning = false
    }

    private fun sendToNetworks(savedEpisodes: List<EpisodeVariant>) {
        if (savedEpisodes.isEmpty()) {
            return
        }

        val animeMap = savedEpisodes.groupBy { it.mapping!!.anime!!.uuid!! }

        for ((_, episodes) in animeMap) {
            val nonSavedEpisodes = episodes.filter { !typeIdentifiersWithPlatforms.contains(getTypeIdentifierWithPlatform(it)) }

            if (nonSavedEpisodes.size >= configCacheService.getValueAsInt(ConfigPropertyKey.SOCIAL_NETWORK_EPISODES_SIZE_LIMIT)) {
                continue
            }

            for (episode in nonSavedEpisodes) {
                typeIdentifiersWithPlatforms.add(getTypeIdentifierWithPlatform(episode))
                val episodeDto = AbstractConverter.convert(episode, EpisodeVariantDto::class.java)
                sendToSocialNetworks(episodeDto)
                sendEpisodeNotification(episode, episodeDto)
            }
        }
    }

    private fun sendEpisodeNotification(
        episode: EpisodeVariant,
        episodeDto: EpisodeVariantDto
    ) {
        val typeIdentifierWithoutPlatform = getTypeIdentifierWithoutPlatform(episode)

        if (typeIdentifiersWithoutPlatforms.contains(typeIdentifierWithoutPlatform)) {
            return
        }

        typeIdentifiersWithoutPlatforms.add(typeIdentifierWithoutPlatform)

        try {
            FirebaseNotification.send(episodeDto)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while sending notification for episode ${episodeDto.identifier}", e)
        }
    }

    private fun getTypeIdentifierWithPlatform(tuple: Tuple): String {
        val country = tuple[0] as CountryCode
        val anime = tuple[1] as UUID
        val platform = tuple[2] as Platform
        val episodeType = tuple[3] as EpisodeType
        val season = tuple[4] as Int
        val number = tuple[5] as Int
        val audioLocale = tuple[6] as String

        val langType = LangType.fromAudioLocale(country, audioLocale)
        return "${country}_${anime}_${platform}_${episodeType}_${season}_${number}_${langType}"
    }

    private fun getTypeIdentifierWithoutPlatform(tuple: Tuple): String {
        val country = tuple[0] as CountryCode
        val anime = tuple[1] as UUID
        val episodeType = tuple[3] as EpisodeType
        val season = tuple[4] as Int
        val number = tuple[5] as Int
        val audioLocale = tuple[6] as String

        val langType = LangType.fromAudioLocale(country, audioLocale)
        return "${country}_${anime}_${episodeType}_${season}_${number}_${langType}"
    }

    private fun getTypeIdentifierWithPlatform(episodeVariant: EpisodeVariant): String {
        val country = episodeVariant.mapping!!.anime!!.countryCode!!
        val anime = episodeVariant.mapping!!.anime!!.uuid!!
        val platform = episodeVariant.platform!!
        val episodeType = episodeVariant.mapping!!.episodeType!!
        val season = episodeVariant.mapping!!.season!!
        val number = episodeVariant.mapping!!.number!!
        val audioLocale = episodeVariant.audioLocale!!

        val langType = LangType.fromAudioLocale(country, audioLocale)
        return "${country}_${anime}_${platform}_${episodeType}_${season}_${number}_${langType}"
    }


    private fun getTypeIdentifierWithoutPlatform(episodeVariant: EpisodeVariant): String {
        val country = episodeVariant.mapping!!.anime!!.countryCode!!
        val anime = episodeVariant.mapping!!.anime!!.uuid!!
        val episodeType = episodeVariant.mapping!!.episodeType!!
        val season = episodeVariant.mapping!!.season!!
        val number = episodeVariant.mapping!!.number!!
        val audioLocale = episodeVariant.audioLocale!!

        val langType = LangType.fromAudioLocale(country, audioLocale)
        return "${country}_${anime}_${episodeType}_${season}_${number}_${langType}"
    }

    private fun sendToSocialNetworks(dto: EpisodeVariantDto) {
        val mediaImage = try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(ImageService.toEpisodeImage(dto), "jpg", byteArrayOutputStream)
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