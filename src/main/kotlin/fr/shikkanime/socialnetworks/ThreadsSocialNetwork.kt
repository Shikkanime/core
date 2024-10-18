package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.wrappers.OldThreadsWrapper
import fr.shikkanime.wrappers.ThreadsWrapper
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import java.util.logging.Level

class ThreadsSocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(ThreadsSocialNetwork::class.java)
    private val oldThreadsWrapper = OldThreadsWrapper()

    private var isInitialized = false
    private var initializedAt: ZonedDateTime? = null

    private var username: String? = null
    private var deviceId: String? = null
    private var token: String? = null
    private var userId: String? = null

    override fun utmSource() = "threads"

    override fun login() {
        if (isInitialized) return

        try {
            if (configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_NEW_THREADS_WRAPPER)) {
                this.token = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_ACCESS_TOKEN))
            } else {
                val username = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_USERNAME))
                val password = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_PASSWORD))
                if (username.isBlank() || password.isBlank()) throw Exception("Username or password is empty")
                val generateDeviceId = oldThreadsWrapper.generateDeviceId(username, password)
                val (token, userId) = runBlocking { oldThreadsWrapper.login(generateDeviceId, username, password) }

                this.username = username
                this.deviceId = generateDeviceId
                this.token = token
                this.userId = userId
            }

            isInitialized = true
            initializedAt = ZonedDateTime.now()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while initializing ThreadsSocialNetwork", e)
        }
    }

    override fun logout() {
        if (!isInitialized) return
        deviceId = null
        token = null
        userId = null
        isInitialized = false
    }

    private fun checkSession() {
        val useNewWrapper = configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_NEW_THREADS_WRAPPER)
        val sessionTimeout = configCacheService.getValueAsInt(ConfigPropertyKey.THREADS_SESSION_TIMEOUT, 10).toLong()

        if (useNewWrapper && isInitialized) return
        if (!useNewWrapper && isInitialized && initializedAt?.plusMinutes(sessionTimeout)
                ?.isAfter(ZonedDateTime.now()) == true
        ) return

        logout()
        login()
    }

    override fun platformAccount(platform: Platform): String {
        return when (platform) {
            Platform.CRUN -> "@crunchyroll_fr"
            Platform.DISN -> "@disneyplus"
            Platform.NETF -> "@netflixfr"
            Platform.PRIM -> "@primevideofr"
            else -> platform.platformName
        }
    }

    override fun sendEpisodeRelease(episodeDto: EpisodeVariantDto, mediaImage: ByteArray?) {
        checkSession()
        if (!isInitialized) return
        val message =
            getEpisodeMessage(
                episodeDto,
                configCacheService.getValueAsString(ConfigPropertyKey.THREADS_FIRST_MESSAGE) ?: ""
            )

        runBlocking {
            if (configCacheService.getValueAsBoolean(ConfigPropertyKey.USE_NEW_THREADS_WRAPPER)) {
                val firstPost = ThreadsWrapper.post(
                    token!!,
                    ThreadsWrapper.PostType.IMAGE,
                    message,
                    imageUrl = "${Constant.apiUrl}/v1/episode-mappings/${episodeDto.uuid}/media-image",
                    altText = "Image de l'épisode ${episodeDto.mapping.number} de ${episodeDto.mapping.anime.shortName}"
                )

                val secondMessage = configCacheService.getValueAsString(ConfigPropertyKey.THREADS_SECOND_MESSAGE)

                if (!secondMessage.isNullOrBlank()) {
                    ThreadsWrapper.post(
                        token!!,
                        ThreadsWrapper.PostType.TEXT,
                        getEpisodeMessage(episodeDto, secondMessage),
                        replyToId = firstPost
                    )
                }
            } else {
                oldThreadsWrapper.publish(username!!, deviceId!!, userId!!, token!!, message, mediaImage)
            }
        }
    }
}