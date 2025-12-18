package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.MailService
import fr.shikkanime.services.MediaImage
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.*
import jakarta.persistence.Tuple
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Level
import javax.imageio.ImageIO

class FetchEpisodesJob(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var isInitialized = false
    private var isRunning = false
    private var lock = 0
    private val maxLock = 15

    private val typeIdentifiers = mutableSetOf<String>()

    @Inject private lateinit var episodeVariantService: EpisodeVariantService
    @Inject private lateinit var configCacheService: ConfigCacheService
    @Inject private lateinit var mailService: MailService

    override suspend fun run() {
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
            variants.forEach { typeIdentifiers.add(getTypeIdentifier(it)) }
            isInitialized = true
        }

        val zonedDateTime = ZonedDateTime.now().withNano(0).withUTC()
        val episodes = mutableListOf<AbstractPlatform.Episode>()

        Constant.abstractPlatforms.sortedBy { it.getPlatform().sortIndex }
            .forEach { abstractPlatform ->
                logger.info("Fetching episodes for ${abstractPlatform.getPlatform().name}...")

                try {
                    episodes.addAll(abstractPlatform.fetchEpisodes(zonedDateTime))
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error while fetching episodes for ${abstractPlatform.getPlatform().name}", e)
                }
            }

        if (episodes.isEmpty()) {
            isRunning = false
            return
        }

        val identifiers = episodeVariantService.findAllIdentifiers()

        val savedEpisodes = episodes.asSequence()
            .sortedWith(
                compareBy(
                    { it.releaseDateTime },
                    { it.anime },
                    { it.season },
                    { it.episodeType },
                    { it.number },
                    { LangType.fromAudioLocale(it.countryCode, it.audioLocale) },
                    { it.platform.sortIndex }
                )
            )
            .filter { zonedDateTime.isAfterOrEqual(it.releaseDateTime) && it.getIdentifier() !in identifiers }
            .mapNotNull {
                try {
                    val savedEpisode = episodeVariantService.save(
                        it,
                        async = configCacheService.getValueAsBoolean(ConfigPropertyKey.ASYNC_FETCH_EPISODE_IMAGES, true)
                    )
                    Constant.abstractPlatforms.forEach { abstractPlatform -> abstractPlatform.updateAnimeSimulcastConfiguration(it.animeId) }
                    savedEpisode
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error while saving episode ${it.getIdentifier()} (${it.anime})", e)
                    null
                }
            }
            .toList()

        isRunning = false

        if (savedEpisodes.isEmpty()) return

        InvalidationService.invalidate(
            Anime::class.java,
            EpisodeMapping::class.java,
            EpisodeVariant::class.java,
            Simulcast::class.java
        )

        sendToNetworks(savedEpisodes)
    }

    private fun getTypeIdentifier(tuple: Tuple): String {
        val countryCode = tuple[0, CountryCode::class.java]
        val langType = LangType.fromAudioLocale(countryCode, tuple[5, String::class.java])
        return "${countryCode}_${tuple[1, UUID::class.java]}_${tuple[2, Int::class.java]}_${tuple[3, EpisodeType::class.java]}_${tuple[4, Int::class.java]}_$langType"
    }

    private fun getTypeIdentifier(episodeVariant: EpisodeVariant): String {
        val countryCode = episodeVariant.mapping!!.anime!!.countryCode!!
        val langType = LangType.fromAudioLocale(countryCode, episodeVariant.audioLocale!!)
        return "${countryCode}_${episodeVariant.mapping!!.anime!!.uuid!!}_${episodeVariant.mapping!!.season!!}_${episodeVariant.mapping!!.episodeType!!}_${episodeVariant.mapping!!.number!!}_$langType"
    }

    private suspend fun sendToNetworks(savedEpisodes: List<EpisodeVariant>) {
        savedEpisodes
            .groupBy { it.mapping?.anime?.uuid }
            .values
            .forEach { episodes ->
                episodes.filter { typeIdentifiers.add(getTypeIdentifier(it)) }
                    .groupBy { it.mapping!!.episodeType!! }
                    .forEach { (_, variants) ->
                        sendToSocialNetworks(variants)
                    }
            }
    }

    private suspend fun sendToSocialNetworks(episodes: List<EpisodeVariant>) {
        val mediaImage = try {
            val byteArrayOutputStream = ByteArrayOutputStream()

            withContext(coroutineDispatcher) {
                ImageIO.write(MediaImage.toMediaImage(*episodes.toTypedArray()), "jpg", byteArrayOutputStream)
            }

            byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while converting episode image for social networks", e)
            null
        }

        Constant.abstractSocialNetworks.forEach { socialNetwork ->
            try {
                socialNetwork.sendEpisodeRelease(episodes, mediaImage)
            } catch (e: Exception) {
                val title = "Error while sending episode release for ${socialNetwork.javaClass.simpleName.replace("SocialNetwork",
                    StringUtils.EMPTY_STRING)}"
                logger.log(Level.SEVERE, title, e)
                val stringWriter = StringWriter()
                e.printStackTrace(PrintWriter(stringWriter))

                mailService.saveAdminMail(
                    title = title,
                    body = stringWriter.toString().replace("\n", "<br>")
                )
            }
        }
    }
}