package fr.shikkanime.socialnetworks

import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.ThreadsWrapper
import kotlinx.coroutines.runBlocking
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.logging.Level

class ThreadsSocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(ThreadsSocialNetwork::class.java)
    private var token: String? = null
    private var isInitialized = false

    override fun login() {
        if (isInitialized) return

        try {
            this.token = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_ACCESS_TOKEN))
            isInitialized = true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while initializing ThreadsSocialNetwork", e)
        }
    }

    override fun logout() {
        if (!isInitialized) return
        token = null
        isInitialized = false
    }

    private fun checkSession() {
        if (isInitialized) return

        logout()
        login()
    }

    override fun sendEpisodeRelease(variants: List<EpisodeVariant>, mediaImage: ByteArray?) {
        require(variants.isNotEmpty()) { "Variants must not be empty" }
        require(variants.map { it.mapping!!.anime!!.uuid }.distinct().size == 1) { "All variants must be from the same anime" }

        checkSession()
        if (!isInitialized) return
        val message =
            getEpisodeMessage(
                variants,
                configCacheService.getValueAsString(ConfigPropertyKey.THREADS_FIRST_MESSAGE) ?: ""
            )

        runBlocking {
            val firstPost = ThreadsWrapper.post(
                    token!!,
                    ThreadsWrapper.PostType.IMAGE,
                    message,
                    imageUrl = "${Constant.apiUrl}/v1/episode-mappings/media-image?uuids=${URLEncoder.encode(EncryptionManager.toGzip(variants.joinToString(",") { it.uuid.toString() }),
                        StandardCharsets.UTF_8)}",
                    altText = "Image de l'épisode ${variants.first().mapping!!.number} de ${StringUtils.getShortName(variants.first().mapping!!.anime!!.name!!)}"
                )

                val secondMessage = configCacheService.getValueAsString(ConfigPropertyKey.THREADS_SECOND_MESSAGE)

                if (!secondMessage.isNullOrBlank()) {
                    ThreadsWrapper.post(
                        token!!,
                        ThreadsWrapper.PostType.TEXT,
                        getEpisodeMessage(variants, secondMessage),
                        replyToId = firstPost
                    )
                }
        }
    }
}