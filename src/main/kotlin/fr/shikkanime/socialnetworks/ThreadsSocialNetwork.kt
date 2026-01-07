package fr.shikkanime.socialnetworks

import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.wrappers.ThreadsWrapper
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.logging.Level

class ThreadsSocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(ThreadsSocialNetwork::class.java)
    private var token: String? = null
    private var isInitialized = false

    override val priority: Int
        get() = 4

    override fun login() {
        if (isInitialized) return

        if (!configCacheService.getValueAsBoolean(ConfigPropertyKey.THREADS_ENABLED)) {
            logger.info("Threads is disabled in configuration")
            return
        }

        try {
            this.token = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.THREADS_ACCESS_TOKEN))
            require(token!!.isNotBlank()) { "Threads access token is empty" }
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

    override suspend fun sendEpisodeRelease(groupedEpisodes: List<GroupedEpisode>, mediaImage: ByteArray?) {
        checkSession()
        if (!isInitialized) return

        val message =
            getEpisodeMessage(
                groupedEpisodes,
                configCacheService.getValueAsString(
                    if (groupedEpisodes.size == 1) ConfigPropertyKey.THREADS_FIRST_MESSAGE
                    else ConfigPropertyKey.THREADS_MULTIPLE_MESSAGE
                ) ?: StringUtils.EMPTY_STRING
            )

        val allVariants = groupedEpisodes.flatMap { it.variants }
        val uuids = allVariants.joinToString(StringUtils.COMMA_STRING) { it.uuid.toString() }
        val encryptedUuids = URLEncoder.encode(EncryptionManager.toGzip(uuids), StandardCharsets.UTF_8)

        val altText = if (groupedEpisodes.size == 1) {
            val firstGe = groupedEpisodes.first()
            "Image de l'épisode ${firstGe.minNumber} de ${StringUtils.getShortName(firstGe.anime.name!!)}"
        } else {
            "Image des nouveaux épisodes"
        }

        val firstPost = ThreadsWrapper.post(
            token!!,
            ThreadsWrapper.PostType.IMAGE,
            message,
            imageUrl = "${Constant.apiUrl}/v1/episode-mappings/media-image?uuids=$encryptedUuids",
            altText = altText
        )

        val secondMessage = configCacheService.getValueAsString(ConfigPropertyKey.THREADS_SECOND_MESSAGE)

        if (groupedEpisodes.size == 1 && !secondMessage.isNullOrBlank()) {
            ThreadsWrapper.post(
                token!!,
                ThreadsWrapper.PostType.TEXT,
                getEpisodeMessage(groupedEpisodes, secondMessage),
                replyToId = firstPost
            )
        }
    }
}