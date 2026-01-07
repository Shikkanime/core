package fr.shikkanime.socialnetworks

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.StringUtils
import java.io.File
import java.io.FileInputStream
import java.util.logging.Level

class FirebaseSocialNetwork : AbstractSocialNetwork() {
    private val logger = LoggerFactory.getLogger(FirebaseSocialNetwork::class.java)
    private var isInitialized = false

    @Inject
    private lateinit var memberService: MemberService

    override val priority: Int
        get() = 0

    override fun login() {
        if (isInitialized)
            return

        if (!configCacheService.getValueAsBoolean(ConfigPropertyKey.FIREBASE_ENABLED)) {
            logger.info("Firebase is disabled in configuration")
            return
        }

        try {
            val file = File(Constant.dataFolder, "firebase.json")
            if (!file.exists())
                return

            FirebaseApp.initializeApp(
                FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(FileInputStream(file)))
                    .build()
            )

            isInitialized = true
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error while initializing FirebaseSocialNetwork", e)
        }
    }

    override fun logout() {
        // Do nothing
    }

    override suspend fun sendEpisodeRelease(groupedEpisodes: List<GroupedEpisode>, mediaImage: ByteArray?) {
        login()
        if (!isInitialized) return
        logger.info("Sending notification...")

        val messages = groupedEpisodes.flatMap { groupedEpisode ->
            val image = "${Constant.apiUrl}/v1/attachments?uuid=${groupedEpisode.mappings.first()}&type=${ImageType.BANNER.name}"

            val notification = Notification.builder()
                .setTitle(StringUtils.getShortName(groupedEpisode.anime.name!!))
                .setBody(StringUtils.toVariantsString(*groupedEpisode.variants.toTypedArray()))
                .setImage(image)
                .build()

            val androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder().setChannelId("high_importance_channel").setImage(image).build())
                .build()

            val apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder().setContentAvailable(true).build())
                .setFcmOptions(ApnsFcmOptions.builder().setImage(image).build())
                .putAllHeaders(mapOf("apns-priority" to "10"))
                .build()

            val topics = mutableSetOf("global").apply {
                memberService.findAllByAnimeUUID(groupedEpisode.anime.uuid!!)
                    .forEach { add(it.uuid.toString()) }
            }

            topics.map { topic ->
                Message.builder()
                    .setTopic(topic)
                    .setNotification(notification)
                    .setAndroidConfig(androidConfig)
                    .setApnsConfig(apnsConfig)
                    .build()
            }
        }

        logger.info("Sending ${messages.size} messages...")
        // 500 messages is the limit for Firebase send each
        messages.chunked(500).forEach(FirebaseMessaging.getInstance()::sendEach)
        logger.info("All messages have been dispatched.")
    }
}