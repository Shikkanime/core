package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import twitter4j.v2
import java.io.ByteArrayInputStream
import java.util.*
import java.util.logging.Level

class TwitterSocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(TwitterSocialNetwork::class.java)
    private var isInitialized = false
    private var twitter: Twitter? = null

    override fun login() {
        if (isInitialized) return

        try {
            val consumerKey = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_CONSUMER_KEY))
            val consumerSecret = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_CONSUMER_SECRET))
            val accessToken = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_ACCESS_TOKEN))
            val accessTokenSecret = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_ACCESS_TOKEN_SECRET))
            if (consumerKey.isBlank() || consumerSecret.isBlank() || accessToken.isBlank() || accessTokenSecret.isBlank()) throw Exception("Twitter credentials are empty")

            twitter = TwitterFactory(
                ConfigurationBuilder()
                    .setDebugEnabled(true)
                    .setOAuthConsumerKey(consumerKey)
                    .setOAuthConsumerSecret(consumerSecret)
                    .setOAuthAccessToken(accessToken)
                    .setOAuthAccessTokenSecret(accessTokenSecret)
                    .build()
            ).instance

            isInitialized = true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while initializing TwitterSocialNetwork", e)
        }
    }

    override fun logout() {
        if (!isInitialized) return

        try {
            twitter = null
            isInitialized = false
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while shutting down TwitterSocialNetwork", e)
        }
    }

    override fun sendMessage(message: String) {
        login()
        if (!isInitialized) return
        if (twitter == null) return

        twitter!!.v2.createTweet(text = message)
    }

    private fun platformAccount(platform: Platform): String {
        return when (platform) {
            Platform.ANIM -> "@ADNanime"
            Platform.CRUN -> "@Crunchyroll_fr"
            Platform.NETF -> "@NetflixFR"
            Platform.DISN -> "@DisneyPlusFR"
            Platform.PRIM -> "@PrimeVideoFR"
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

        var configMessage = configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_MESSAGE) ?: ""
        configMessage = configMessage.replace("{SHIKKANIME_URL}", "https://www.shikkanime.fr/animes/${episodeDto.anime.slug}")
        configMessage = configMessage.replace("{URL}", episodeDto.url)
        configMessage = configMessage.replace("{PLATFORM_ACCOUNT}", platformAccount(episodeDto.platform))
        configMessage = configMessage.replace("{ANIME_HASHTAG}", "#${StringUtils.getHashtag(episodeDto.anime.shortName)}")
        configMessage = configMessage.replace("{ANIME_TITLE}", episodeDto.anime.shortName)
        configMessage = configMessage.replace("{EPISODE_INFORMATION}", "${information(episodeDto)}${uncensored}")
        configMessage = configMessage.replace("{VOICE}", isVoice)
        configMessage = configMessage.replace("\\n", "\n")
        configMessage = configMessage.trim()
        return configMessage
    }

    override fun sendEpisodeRelease(episodeDto: EpisodeDto, mediaImage: ByteArray) {
        login()
        if (!isInitialized) return
        if (twitter == null) return
        val message = getMessage(episodeDto)

        val uploadMedia = twitter!!.tweets().uploadMedia(
            UUID.randomUUID().toString(),
            ByteArrayInputStream(mediaImage)
        )

        twitter!!.v2.createTweet(mediaIds = arrayOf(uploadMedia.mediaId), text = message)
    }
}