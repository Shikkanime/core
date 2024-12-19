package fr.shikkanime.jobs

import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.Simulcast
import fr.shikkanime.entities.enums.*
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.services.EmailService
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.MediaImage
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.utils.*
import jakarta.inject.Inject
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.io.StringWriter
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
    private lateinit var episodeVariantCacheService: EpisodeVariantCacheService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    @Inject
    private lateinit var emailService: EmailService

    fun addHashCaches(
        it: AbstractPlatform<*, *, *>,
        variants: List<EpisodeVariant>
    ) {
        if (it.getPlatform() == Platform.DISN) {
            val episodeVariants = variants.filter { variant -> variant.platform == it.getPlatform() }

            it.hashCache.addAll(episodeVariants.map { variant ->
                ".{2}-.{4}-(.*)-.{2}-.{2}".toRegex().find(variant.identifier!!)!!.groupValues[1]
            })

            it.hashCache.addAll(episodeVariants.map { variant ->
                EncryptionManager.toSHA512("${variant.mapping!!.anime!!.name}-${variant.mapping!!.season}-${variant.mapping!!.number}")
                    .substring(0..<8)
            })
        }
    }

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
            identifiers.addAll(episodeVariantCacheService.findAllIdentifiers())
            val variants = episodeVariantCacheService.findAll()
            Constant.abstractPlatforms.forEach { addHashCaches(it, variants) }
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

        MapCache.invalidate(
            Anime::class.java,
            EpisodeMapping::class.java,
            EpisodeVariant::class.java,
            Simulcast::class.java
        )

        sendToNetworks(savedEpisodes)
    }

    private fun getTypeIdentifier(episodeVariant: EpisodeVariant): String {
        val langType = LangType.fromAudioLocale(episodeVariant.mapping!!.anime!!.countryCode!!, episodeVariant.audioLocale!!)
        return "${episodeVariant.mapping!!.anime!!.countryCode!!}_${episodeVariant.mapping!!.anime!!.uuid!!}_${episodeVariant.mapping!!.season!!}_${episodeVariant.mapping!!.episodeType!!}_${episodeVariant.mapping!!.number!!}_$langType"
    }

    private fun sendToNetworks(savedEpisodes: List<EpisodeVariant>) {
        val sizeLimit = configCacheService.getValueAsInt(ConfigPropertyKey.SOCIAL_NETWORK_EPISODES_SIZE_LIMIT)

        savedEpisodes
            .groupBy { it.mapping?.anime?.uuid }
            .values
            .forEach { episodes ->
                episodes.filter { typeIdentifiers.add(getTypeIdentifier(it)) }
                    .takeIf { it.size < sizeLimit }
                    ?.groupBy { it.mapping?.uuid }
                    ?.forEach { _, episodes ->
                        val dtos = AbstractConverter.convert(episodes, EpisodeVariantDto::class.java)!!
                        sendToSocialNetworks(dtos)
                    }
            }
    }

    private fun sendToSocialNetworks(episodes: List<EpisodeVariantDto>) {
        val mediaImage = try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(MediaImage.toMediaImage(episodes), "jpg", byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while converting episode image for social networks", e)
            null
        }

        Constant.abstractSocialNetworks.parallelStream().forEach { socialNetwork ->
            try {
                socialNetwork.sendEpisodeRelease(episodes, mediaImage)
            } catch (e: Exception) {
                val title = "Error while sending episode release for ${socialNetwork.javaClass.simpleName.replace("SocialNetwork", "")}"
                logger.log(Level.SEVERE, title, e)
                val stringWriter = StringWriter()
                e.printStackTrace(PrintWriter(stringWriter))
                emailService.sendAdminEmail(title, stringWriter.toString().replace("\n", "<br>"))
            }
        }
    }
}