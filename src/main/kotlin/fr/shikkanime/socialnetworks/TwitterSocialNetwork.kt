package fr.shikkanime.socialnetworks

import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.services.ImageService
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import twitter4j.v2
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.logging.Level
import javax.imageio.ImageIO

class TwitterSocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(TwitterSocialNetwork::class.java)
    private var isInitialized = false
    private var twitter: Twitter? = null

    override fun login() {
        if (isInitialized) return

        try {
            twitter = TwitterFactory(
                ConfigurationBuilder()
                    .setDebugEnabled(true)
                    .setOAuthConsumerKey(configService.getValueAsString("twitter_consumer_key"))
                    .setOAuthConsumerSecret(configService.getValueAsString("twitter_consumer_secret"))
                    .setOAuthAccessToken(configService.getValueAsString("twitter_access_token"))
                    .setOAuthAccessTokenSecret(configService.getValueAsString("twitter_access_token_secret"))
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

        try {
            twitter!!.v2.createTweet(text = message)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while sending message to Twitter", e)
        }
    }

    private fun platformAccount(platform: Platform): String {
        return when (platform) {
            Platform.ANIM -> "@ADNanime"
            Platform.CRUN -> "@Crunchyroll_fr"
            Platform.NETF -> "@NetflixFR"
            Platform.DISN -> "@DisneyPlusFR"
        }
    }

    private fun information(episodeDto: EpisodeDto): String {
        return when (episodeDto.episodeType) {
            EpisodeType.SPECIAL -> "L'épisode spécial"
            EpisodeType.FILM -> "Le film"
            else -> "L'épisode ${episodeDto.number}"
        }
    }

    override fun sendEpisodeRelease(episodeDto: EpisodeDto) {
        login()
        if (!isInitialized) return
        if (twitter == null) return

        try {
            val uncensored = if (episodeDto.uncensored) " non censuré" else ""
            val isVoice = if (episodeDto.langType == LangType.VOICE) " en VF " else " "
            val message =
                "\uD83D\uDEA8 ${information(episodeDto)}${uncensored} de #${StringUtils.getHashtag(episodeDto.anime.shortName)} est maintenant disponible${isVoice}sur ${
                    platformAccount(episodeDto.platform)
                }\n" +
                        "\n" +
                        "Bon visionnage. \uD83C\uDF7F\n" +
                        "\n" +
                        "\uD83D\uDD36 Lien de l'épisode : ${episodeDto.url}"

            val byteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(ImageService.toEpisodeImage(episodeDto), "png", byteArrayOutputStream)

            val uploadMedia = twitter!!.tweets().uploadMedia(
                UUID.randomUUID().toString(),
                ByteArrayInputStream(byteArrayOutputStream.toByteArray())
            )

            twitter!!.v2.createTweet(mediaIds = arrayOf(uploadMedia.mediaId), text = message)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while sending message to Twitter", e)
        }
    }
}