package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.utils.LoggerFactory
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

    override fun utmSource() = "twitter"

    override fun login() {
        if (isInitialized) return

        try {
            val consumerKey =
                requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_CONSUMER_KEY))
            val consumerSecret =
                requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_CONSUMER_SECRET))
            val accessToken =
                requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_ACCESS_TOKEN))
            val accessTokenSecret =
                requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_ACCESS_TOKEN_SECRET))
            if (consumerKey.isBlank() || consumerSecret.isBlank() || accessToken.isBlank() || accessTokenSecret.isBlank()) throw Exception(
                "Twitter credentials are empty"
            )

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

    override fun platformAccount(platform: PlatformDto): String {
        return when (platform.id) {
            "ANIM" -> "@ADNanime"
            "CRUN" -> "@Crunchyroll_fr"
            "NETF" -> "@NetflixFR"
            "DISN" -> "@DisneyPlusFR"
            "PRIM" -> "@PrimeVideoFR"
            else -> platform.name
        }
    }

    override fun sendEpisodeRelease(episodeDto: EpisodeVariantDto, mediaImage: ByteArray) {
        login()
        if (!isInitialized) return
        if (twitter == null) return
        val message =
            getEpisodeMessage(episodeDto, configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_MESSAGE) ?: "")

        val uploadMedia = twitter!!.tweets().uploadMedia(
            UUID.randomUUID().toString(),
            ByteArrayInputStream(mediaImage)
        )

        twitter!!.v2.createTweet(mediaIds = arrayOf(uploadMedia.mediaId), text = message)
    }

    override fun sendCalendar(message: String, calendarImage: ByteArray) {
        login()
        if (!isInitialized) return
        if (twitter == null) return

        val uploadMedia = twitter!!.tweets().uploadMedia(
            UUID.randomUUID().toString(),
            ByteArrayInputStream(calendarImage)
        )

        twitter!!.v2.createTweet(mediaIds = arrayOf(uploadMedia.mediaId), text = message)
    }
}