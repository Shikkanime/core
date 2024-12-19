package fr.shikkanime.socialnetworks

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import com.google.inject.Inject
import fr.shikkanime.dtos.variants.EpisodeVariantDto
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

    override fun utmSource() = "firebase"

    override fun login() {
        if (isInitialized)
            return

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
        if (!isInitialized) return
        isInitialized = false
    }

    override fun sendEpisodeRelease(episodes: List<EpisodeVariantDto>, mediaImage: ByteArray?) {
        login()
        if (!isInitialized) return

        val mapping = episodes.first().mapping
        val image = "${Constant.apiUrl}/v1/attachments?uuid=${mapping.uuid}&type=image"

        val notification = Notification.builder()
            .setTitle(mapping.anime.shortName)
            .setBody(StringUtils.toEpisodeVariantString(episodes))
            .setImage(image)
            .build()

        val androidConfig = AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .build()

        val apnsConfig = ApnsConfig.builder()
            .setAps(Aps.builder().setContentAvailable(false).build())
            .setFcmOptions(ApnsFcmOptions.builder().setImage(image).build())
            .putAllHeaders(mapOf("apns-priority" to "10"))
            .build()

        val topics = mutableSetOf("global")
        memberService.findAllByAnimeUUID(mapping.anime.uuid!!).forEach { topics.add(it.uuid!!.toString()) }
        // Chunked topics to avoid the 500 topics limit (due to the limit of the Firebase API)
        val chunkedTopics = topics.chunked(500)

        chunkedTopics.forEach {
            FirebaseMessaging.getInstance()
                .sendEach(
                    it.map { topic ->
                        Message.builder()
                            .setTopic(topic)
                            .setNotification(notification)
                            .setAndroidConfig(androidConfig)
                            .setApnsConfig(apnsConfig)
                            .build()
                    }
                )
        }
    }
}