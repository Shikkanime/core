package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.wrappers.ThreadsWrapper
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import java.util.logging.Level

class ThreadsSocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(ThreadsSocialNetwork::class.java)

    private var isInitialized = false
    private var initializedAt: ZonedDateTime? = null

    private var username: String? = null
    private var deviceId: String? = null
    private var token: String? = null
    private var userId: String? = null

    override fun login() {
        if (isInitialized) return

        try {
            val username = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_USERNAME))
            val password = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_PASSWORD))

            val generateDeviceId = ThreadsWrapper.generateDeviceId(username, password)
            val (token, userId) = runBlocking { ThreadsWrapper.login(generateDeviceId, username, password) }

            this.username = username
            this.deviceId = generateDeviceId
            this.token = token
            this.userId = userId
            isInitialized = true
            initializedAt = ZonedDateTime.now()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while initializing BskySocialNetwork", e)
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
        if (!isInitialized) return

        if (initializedAt!!.plusMinutes(10).isBefore(ZonedDateTime.now())) {
            logout()
            login()
        }
    }

    override fun sendMessage(message: String) {
        checkSession()
        if (!isInitialized) return
        runBlocking { ThreadsWrapper.publish(username!!, deviceId!!, userId!!, token!!, message) }
    }

    private fun platformAccount(platform: Platform): String {
        return when (platform) {
            Platform.CRUN -> "@crunchyroll_fr"
            Platform.NETF -> "@netflixfr"
            Platform.PRIM -> "@primevideofr"
            else -> platform.name
        }
    }

    private fun information(episodeDto: EpisodeDto): String {
        return when (episodeDto.episodeType) {
            EpisodeType.SPECIAL -> "L'épisode spécial"
            EpisodeType.FILM -> "Le film"
            else -> "L'épisode ${episodeDto.number}"
        }
    }

    fun getMessage(episodeDto: EpisodeDto): String {
        val uncensored = if (episodeDto.uncensored) " non censuré" else ""
        val isVoice = if (episodeDto.langType == LangType.VOICE) " en VF " else " "

        var configMessage = configCacheService.getValueAsString(ConfigPropertyKey.THREADS_MESSAGE) ?: ""
        configMessage = configMessage.replace("{URL}", episodeDto.url)
        configMessage = configMessage.replace("{PLATFORM_ACCOUNT}", platformAccount(episodeDto.platform))
        configMessage = configMessage.replace("{ANIME_TITLE}", episodeDto.anime.shortName)
        configMessage = configMessage.replace("{EPISODE_INFORMATION}", "${information(episodeDto)}${uncensored}")
        configMessage = configMessage.replace("{VOICE}", isVoice)
        configMessage = configMessage.replace("\\n", "\n")
        configMessage = configMessage.trim()
        return configMessage
    }

    override fun sendEpisodeRelease(episodeDto: EpisodeDto, mediaImage: ByteArray) {
        checkSession()
        if (!isInitialized) return

        val message = getMessage(episodeDto)
        runBlocking { ThreadsWrapper.publish(username!!, deviceId!!, userId!!, token!!, message, mediaImage) }
    }
}