package fr.shikkanime.utils

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import fr.shikkanime.dtos.variants.EpisodeVariantDto
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

        FirebaseMessaging.getInstance()
            .send(
                Message.builder()
                    .setNotification(
                        Notification.builder()
                            .setTitle(episodeDto.mapping.anime.shortName)
                            .setBody(StringUtils.toEpisodeString(episodeDto))
                            .setImage("${Constant.apiUrl}/v1/attachments?uuid=${episodeDto.mapping.uuid}&type=image")
                            .build()
                    )
                    .setAndroidConfig(
                        AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build()
                    )
                    .setTopic("global")
                    .build()
            )
    }
}