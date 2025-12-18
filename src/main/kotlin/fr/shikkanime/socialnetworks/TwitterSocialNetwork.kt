package fr.shikkanime.socialnetworks

import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.takeIfNotEmpty
import fr.shikkanime.wrappers.TwitterWrapper
import java.util.*
import java.util.logging.Level

class TwitterSocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(TwitterSocialNetwork::class.java)
    private var isInitialized = false
    private var authParams: TwitterWrapper.AuthParams? = null

    override val priority: Int
        get() = 2

    override fun login() {
        if (isInitialized) return

        if (!configCacheService.getValueAsBoolean(ConfigPropertyKey.TWITTER_ENABLED)) {
            logger.info("Twitter is disabled in configuration")
            return
        }

        try {
            val consumerKey = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_CONSUMER_KEY))
            val consumerSecret = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_CONSUMER_SECRET))
            val accessToken = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_ACCESS_TOKEN))
            val accessTokenSecret = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_ACCESS_TOKEN_SECRET))
            require(consumerKey.isNotBlank()) { "Twitter consumer key is empty" }
            require(consumerSecret.isNotBlank()) { "Twitter consumer secret is empty" }
            require(accessToken.isNotBlank()) { "Twitter access token is empty" }
            require(accessTokenSecret.isNotBlank()) { "Twitter access token secret is empty" }

            authParams = TwitterWrapper.AuthParams(
                consumerKey = consumerKey,
                consumerSecret = consumerSecret,
                accessToken = accessToken,
                accessTokenSecret = accessTokenSecret,
            )

            isInitialized = true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while initializing TwitterSocialNetwork", e)
        }
    }

    override fun logout() {
        if (!isInitialized) return

        try {
            authParams = null
            isInitialized = false
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while shutting down TwitterSocialNetwork", e)
        }
    }

    override suspend fun sendEpisodeRelease(variants: List<EpisodeVariant>, mediaImage: ByteArray?) {
        login()
        if (!isInitialized) return
        if (authParams == null) return

        val firstMessage = getEpisodeMessage(
            variants,
            configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_FIRST_MESSAGE) ?: StringUtils.EMPTY_STRING
        )

        val mediaIds = runCatching {
            mediaImage?.let {
                listOf(
                    TwitterWrapper.uploadMediaChunked(
                        authParams!!,
                        TwitterWrapper.MediaCategory.TWEET_IMAGE,
                        TwitterWrapper.MediaType.IMAGE_JPEG,
                        UUID.randomUUID().toString(),
                        it
                    )
                )
            }
        }.getOrNull()?.takeIfNotEmpty()

        val firstTweetId = TwitterWrapper.createTweet(authParams!!, mediaIds = mediaIds, text = firstMessage)
        val secondMessage = configCacheService.getValueAsString(ConfigPropertyKey.TWITTER_SECOND_MESSAGE)

        if (!secondMessage.isNullOrBlank()) {
            TwitterWrapper.createTweet(
                authParams!!,
                inReplyToTweetId = firstTweetId,
                text = getEpisodeMessage(variants, secondMessage)
            )
        }
    }
}