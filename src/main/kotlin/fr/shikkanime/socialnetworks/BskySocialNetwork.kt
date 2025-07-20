package fr.shikkanime.socialnetworks

import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.utils.StringUtils
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

        if (!configCacheService.getValueAsBoolean(ConfigPropertyKey.BSKY_ENABLED)) {
            logger.info("BlueSky is disabled in configuration")
            return
        }

        try {
            val identifier = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.BSKY_IDENTIFIER))
            val password = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.BSKY_PASSWORD))
            if (identifier.isBlank() || password.isBlank()) throw Exception("Identifier or password is empty")
            val session = runBlocking { BskyWrapper.createSession(identifier, password) }
            accessJwt = requireNotNull(session.getAsString("accessJwt"))
            did = requireNotNull(session.getAsString("did"))
            require(accessJwt!!.isNotBlank()) { "Access JWT is empty" }
            require(did!!.isNotBlank()) { "DID is empty" }
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
            ) > ZonedDateTime.now()
        ) {
            return
        }

        logout()
        login()
    }

    override fun sendEpisodeRelease(variants: List<EpisodeVariant>, mediaImage: ByteArray?) {
        checkSession()
        if (!isInitialized) return

        val firstMessage =
            getEpisodeMessage(
                variants,
                configCacheService.getValueAsString(ConfigPropertyKey.BSKY_FIRST_MESSAGE) ?: StringUtils.EMPTY_STRING
            )

        val firstRecord = runBlocking {
            BskyWrapper.createRecord(
                accessJwt!!,
                did!!,
                firstMessage,
                mediaImage?.let {
                    listOf(
                        BskyWrapper.Image(
                            BskyWrapper.uploadBlob(
                                accessJwt!!,
                                ContentType.Image.JPEG,
                                it
                            )
                        )
                    )
                } ?: emptyList()
            )
        }

        val secondMessage = configCacheService.getValueAsString(ConfigPropertyKey.BSKY_SECOND_MESSAGE)

        if (!secondMessage.isNullOrBlank()) {
            runBlocking {
                BskyWrapper.createRecord(
                    accessJwt!!,
                    did!!,
                    getEpisodeMessage(variants, secondMessage.replace("{EMBED}", StringUtils.EMPTY_STRING)).trim(),
                    replyTo = firstRecord,
                    embed = getInternalUrl(variants).takeIf { "{EMBED}" in secondMessage }
                )
            }
        }
    }
}