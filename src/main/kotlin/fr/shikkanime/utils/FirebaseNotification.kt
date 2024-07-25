package fr.shikkanime.utils

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import fr.shikkanime.dtos.variants.EpisodeVariantDto
import fr.shikkanime.services.MemberService
import java.io.File
import java.io.FileInputStream

object FirebaseNotification {
    private var isInitialized = false

    private fun init() {
        if (isInitialized) return
        val file = File(Constant.dataFolder, "firebase.json")
        if (!file.exists()) return

        FirebaseApp.initializeApp(
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(FileInputStream(file)))
                .build()
        )

        isInitialized = true
    }

    fun send(episodeDto: EpisodeVariantDto) {
        init()
        if (!isInitialized) return
        val memberService = Constant.injector.getInstance(MemberService::class.java)

        val notification = Notification.builder()
            .setTitle(episodeDto.mapping.anime.shortName)
            .setBody(StringUtils.toEpisodeString(episodeDto))
            .setImage("${Constant.apiUrl}/v1/attachments?uuid=${episodeDto.mapping.uuid}&type=image")
            .build()

        val androidConfig = AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .build()

        val apnsConfig = ApnsConfig.builder()
            .setAps(Aps.builder().setBadge(1).build())
            .build()

        val topics = mutableSetOf("global")
        memberService.findAllByAnimeUUID(episodeDto.mapping.anime.uuid!!).forEach { topics.add(it.uuid!!.toString()) }
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