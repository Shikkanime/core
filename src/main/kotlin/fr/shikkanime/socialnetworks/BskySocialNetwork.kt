package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.BskyWrapper
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import java.util.logging.Level

class BskySocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(BskySocialNetwork::class.java)

    private var isInitialized = false
    private var initializedAt: ZonedDateTime? = null
    private var accessJwt: String? = null
    private var did: String? = null

    override fun utmSource() = "bsky"

    override fun login() {
        if (isInitialized) return

        try {
            val identifier = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.BSKY_IDENTIFIER))
            val password = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.BSKY_PASSWORD))
            if (identifier.isBlank() || password.isBlank()) throw Exception("Identifier or password is empty")
            val session = runBlocking { BskyWrapper.createSession(identifier, password) }
            accessJwt = requireNotNull(session.getAsString("accessJwt"))
            did = requireNotNull(session.getAsString("did"))

            isInitialized = true
            initializedAt = ZonedDateTime.now()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while initializing BskySocialNetwork", e)
        }
    }

    override fun logout() {
        if (!isInitialized) return
        accessJwt = null
        did = null
        isInitialized = false
    }

    private fun checkSession() {
        if (initializedAt != null && initializedAt!!.plusMinutes(
                configCacheService.getValueAsInt(
                    ConfigPropertyKey.BSKY_SESSION_TIMEOUT,
                    10
                ).toLong()
            )
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
        runBlocking { BskyWrapper.createRecord(accessJwt!!, did!!, message) }
    }

    override fun sendEpisodeRelease(episodeDto: EpisodeDto, mediaImage: ByteArray) {
        checkSession()
        if (!isInitialized) return
        val message =
            getEpisodeMessage(episodeDto, configCacheService.getValueAsString(ConfigPropertyKey.BSKY_MESSAGE) ?: "")
        val webpByteArray = FileManager.encodeToWebP(mediaImage)
        val imageJson =
            runBlocking { BskyWrapper.uploadBlob(accessJwt!!, ContentType.parse("image/webp"), webpByteArray) }
        runBlocking {
            BskyWrapper.createRecord(
                accessJwt!!,
                did!!,
                message,
                listOf(BskyWrapper.Image(imageJson))
            )
        }
    }

    override fun sendCalendar(message: String, calendarImage: ByteArray) {
        checkSession()
        if (!isInitialized) return
        val webpByteArray = FileManager.encodeToWebP(calendarImage)
        val imageJson =
            runBlocking { BskyWrapper.uploadBlob(accessJwt!!, ContentType.parse("image/webp"), webpByteArray) }
        runBlocking {
            BskyWrapper.createRecord(
                accessJwt!!,
                did!!,
                message,
                listOf(BskyWrapper.Image(imageJson))
            )
        }
    }
}