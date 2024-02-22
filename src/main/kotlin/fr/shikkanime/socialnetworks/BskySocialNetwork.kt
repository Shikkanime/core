package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
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

    override fun login() {
        if (isInitialized) return

        try {
            val identifier = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.BSKY_IDENTIFIER))
            val password = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.BSKY_PASSWORD))
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
        if (!isInitialized) return

        if (initializedAt!!.plusMinutes(10).isBefore(ZonedDateTime.now())) {
            logout()
            login()
        }
    }

    override fun sendMessage(message: String) {
        checkSession()
        if (!isInitialized) return
        runBlocking { BskyWrapper.createRecord(accessJwt!!, did!!, message) }
    }

    private fun information(episodeDto: EpisodeDto): String {
        return when (episodeDto.episodeType) {
            EpisodeType.SPECIAL -> "L'épisode spécial"
            EpisodeType.FILM -> "Le film"
            else -> "L'épisode ${episodeDto.number}"
        }
    }

    override fun sendEpisodeRelease(episodeDto: EpisodeDto, mediaImage: ByteArray) {
        checkSession()
        if (!isInitialized) return

        val url = "https://www.shikkanime.fr/animes/${episodeDto.anime.slug}"
        val uncensored = if (episodeDto.uncensored) " non censuré" else ""
        val isVoice = if (episodeDto.langType == LangType.VOICE) " en VF " else " "
        val message =
            "\uD83D\uDEA8 ${information(episodeDto)}${uncensored} de ${episodeDto.anime.shortName} est maintenant disponible${isVoice}sur ${episodeDto.platform.platformName}\n\nBon visionnage. \uD83C\uDF7F"

        val webpByteArray = FileManager.encodeToWebP(mediaImage)
        val imageJson = runBlocking { BskyWrapper.uploadBlob(accessJwt!!, ContentType.parse("image/webp"), webpByteArray) }
        runBlocking { BskyWrapper.createRecord(accessJwt!!, did!!, message, listOf(BskyWrapper.Image(imageJson, url))) }
    }
}