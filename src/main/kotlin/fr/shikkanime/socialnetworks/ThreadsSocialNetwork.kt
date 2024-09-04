package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.wrappers.ThreadsWrapper
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import java.util.logging.Level

class ThreadsSocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(ThreadsSocialNetwork::class.java)
    private val threadsWrapper = ThreadsWrapper()

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
            val username = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_USERNAME))
            val password = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_PASSWORD))
            if (username.isBlank() || password.isBlank()) throw Exception("Username or password is empty")
            val generateDeviceId = threadsWrapper.generateDeviceId(username, password)
            val (token, userId) = runBlocking { threadsWrapper.login(generateDeviceId, username, password) }

            this.username = username
            this.deviceId = generateDeviceId
            this.token = token
            this.userId = userId
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
        if (isInitialized &&
            initializedAt != null &&
            initializedAt!!.plusMinutes(
                configCacheService.getValueAsInt(ConfigPropertyKey.THREADS_SESSION_TIMEOUT, 10).toLong()
            ).isAfter(ZonedDateTime.now())
        ) {
            return
        }

        logout()
        login()
    }

    override fun platformAccount(platform: PlatformDto): String {
        return when (platform.id) {
            "CRUN" -> "@crunchyroll_fr"
            "DISN" -> "@disneyplus"
            "NETF" -> "@netflixfr"
            "PRIM" -> "@primevideofr"
            else -> platform.name
        }
    }

    override fun sendEpisodeRelease(episodeDto: EpisodeVariantDto, mediaImage: ByteArray?) {
        checkSession()
        if (!isInitialized) return
        val message =
            getEpisodeMessage(episodeDto, configCacheService.getValueAsString(ConfigPropertyKey.THREADS_MESSAGE) ?: "")
        runBlocking { threadsWrapper.publish(username!!, deviceId!!, userId!!, token!!, message, mediaImage) }
    }

    override fun sendCalendar(message: String, calendarImage: ByteArray) {
        checkSession()
        if (!isInitialized) return
        runBlocking { threadsWrapper.publish(username!!, deviceId!!, userId!!, token!!, message, calendarImage) }
    }
}