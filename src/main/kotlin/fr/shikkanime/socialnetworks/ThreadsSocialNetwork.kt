package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.wrappers.ThreadsWrapper
import kotlinx.coroutines.runBlocking
import java.util.logging.Level

class ThreadsSocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(ThreadsSocialNetwork::class.java)
    private var token: String? = null
    private var isInitialized = false

    override fun utmSource() = "threads"

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
        }
    }
}