package fr.shikkanime.socialnetworks

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import com.google.inject.Inject
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.ImageType
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
        // Do nothing
    }

    override fun sendEpisodeRelease(variants: List<EpisodeVariant>, mediaImage: ByteArray?) {
        require(variants.isNotEmpty()) { "Variants must not be empty" }
        require(variants.map { it.mapping!!.anime!!.uuid }.distinct().size == 1) { "All variants must be from the same anime" }

        login()
        if (!isInitialized) return

        val anime = variants.first().mapping!!.anime!!

        val mapping = variants.asSequence()
            .map { it.mapping!! }
            .distinctBy { it.uuid!! }
            .sortedWith(compareBy({ it.releaseDateTime}, { it.season }, { it.episodeType }, { it.number }))
            .first()

        val image = "${Constant.apiUrl}/v1/attachments?uuid=${mapping.uuid}&type=${ImageType.BANNER.name}"

        val notification = Notification.builder()
            .setTitle(StringUtils.getShortName(anime.name!!))
            .setBody(StringUtils.toVariantsString(*variants.toTypedArray()))
            .setImage(image)
            .build()

        val androidConfig = AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .build()

        val apnsConfig = ApnsConfig.builder()
            .setAps(Aps.builder().setContentAvailable(true).build())
            .setFcmOptions(ApnsFcmOptions.builder().setImage(image).build())
            .putAllHeaders(mapOf("apns-priority" to "10"))
            .build()

        val topics = mutableSetOf("global")
        memberService.findAllByAnimeUUID(anime.uuid!!).forEach { topics.add(it.uuid!!.toString()) }
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