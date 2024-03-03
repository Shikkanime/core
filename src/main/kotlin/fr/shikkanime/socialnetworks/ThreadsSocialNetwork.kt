package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.Platform
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
        if (initializedAt != null && initializedAt!!.plusMinutes(configCacheService.getValueAsInt(ConfigPropertyKey.THREADS_SESSION_TIMEOUT, 10).toLong())
                .isAfter(ZonedDateTime.now())
        ) {
            return
        }

        logout()
        login()
    }

    override fun sendMessage(message: String) {
        checkSession()
        if (!isInitialized) return
        runBlocking { threadsWrapper.publish(username!!, deviceId!!, userId!!, token!!, message) }
    }

    override fun platformAccount(platform: Platform): String {
        return when (platform) {
            Platform.CRUN -> "@crunchyroll_fr"
            Platform.NETF -> "@netflixfr"
            Platform.PRIM -> "@primevideofr"
            else -> platform.platformName
        }
    }

    override fun sendEpisodeRelease(episodeDto: EpisodeDto, mediaImage: ByteArray) {
        checkSession()
        if (!isInitialized) return
        val message = getEpisodeMessage(episodeDto, configCacheService.getValueAsString(ConfigPropertyKey.THREADS_MESSAGE) ?: "")
        runBlocking { threadsWrapper.publish(username!!, deviceId!!, userId!!, token!!, message, mediaImage) }
    }
}