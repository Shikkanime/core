package fr.shikkanime.socialnetworks

import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.ConfigPropertyKey
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
            val consumerKey =
                requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_CONSUMER_KEY))
            val consumerSecret =
                requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_CONSUMER_SECRET))
            val accessToken =
                requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_ACCESS_TOKEN))
            val accessTokenSecret =
                requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_ACCESS_TOKEN_SECRET))
            require(consumerKey.isNotBlank()) { "Twitter consumer key is empty" }
            require(consumerSecret.isNotBlank()) { "Twitter consumer secret is empty" }
            require(accessToken.isNotBlank()) { "Twitter access token is empty" }
            require(accessTokenSecret.isNotBlank()) { "Twitter access token secret is empty" }

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

    override fun sendEpisodeRelease(variants: List<EpisodeVariant>, mediaImage: ByteArray?) {
        login()
        if (!isInitialized) return
        if (twitter == null) return

        val firstMessage = getEpisodeMessage(
            variants,
            configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_FIRST_MESSAGE) ?: StringUtils.EMPTY_STRING
        )
        val firstTweet = twitter!!.v2.createTweet(mediaIds = mediaImage?.let {
            arrayOf(twitter!!.v2.uploadMedia("image/jpg", UUID.randomUUID().toString(), ByteArrayInputStream(it)).mediaId)
        }, text = firstMessage)
        val secondMessage = configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_SECOND_MESSAGE)

        if (!secondMessage.isNullOrBlank()) {
            twitter!!.v2.createTweet(
                inReplyToTweetId = firstTweet.id,
                text = getEpisodeMessage(variants, secondMessage)
            )
        }
    }
}