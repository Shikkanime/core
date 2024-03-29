package fr.shikkanime.utils

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import fr.shikkanime.dtos.EpisodeDto
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

    fun send(episodeDto: EpisodeDto) {
        init()

        FirebaseMessaging.getInstance()
            .send(
                Message.builder()
                    .setNotification(
                        Notification.builder()
                            .setTitle(episodeDto.anime.shortName)
                            .setBody(StringUtils.toEpisodeString(episodeDto))
                            .setImage("https://api.shikkanime.fr/v1/attachments?uuid=${episodeDto.uuid}&type=image")
                            .build()
                    )
                    .setAndroidConfig(
                        AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build()
                    )
                    .setApnsConfig(
                        ApnsConfig.builder()
                            .putHeader("apns-priority", "5")
                            .build()
                    )
                    .setTopic("global")
                    .build()
            )
    }
}