package fr.shikkanime.utils

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
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

        val tokens = mutableSetOf("global")
        memberService.findAllByAnimeUUID(episodeDto.mapping.anime.uuid!!).forEach { tokens.add(it.uuid!!.toString()) }
        // Chunked tokens to avoid the 500 tokens limit (due to the limit of the Firebase API)
        val chunkedTokens = tokens.chunked(500)

        chunkedTokens.forEach {
            FirebaseMessaging.getInstance()
                .sendEachForMulticast(
                    MulticastMessage.builder()
                        .addAllTokens(it)
                        .setNotification(notification)
                        .setAndroidConfig(androidConfig)
                        .build()
                )
        }
    }
}