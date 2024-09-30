package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

class EmailService {
    data class SMTPConfig(
        val emailHost: String,
        val emailPort: Int,
        val emailUsername: String,
        val emailPassword: String
    )

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    private val cache = MapCache<SMTPConfig, Mailer>(classes = listOf(Config::class.java), log = false) {
        MailerBuilder
            .withSMTPServer(
                it.emailHost,
                it.emailPort,
                it.emailUsername,
                it.emailPassword
            )
            .withTransportStrategy(TransportStrategy.SMTP_TLS)
            .withConnectionPoolCoreSize(10)
            .buildMailer()
    }

    private fun sendEmail(
        emailHost: String,
        emailPort: Int,
        emailUsername: String,
        emailPassword: String,
        email: String,
        title: String,
        body: String
    ) {
        val mailer = cache[SMTPConfig(emailHost, emailPort, emailUsername, emailPassword)] ?: throw Exception("Failed to create mailer")
        mailer.testConnection()

        mailer.sendMail(
            EmailBuilder.startingBlank()
                .from("${Constant.NAME} - Ne pas répondre", emailUsername)
                .to(email)
                .withSubject(title)
                .withHTMLText(body)
                .buildEmail()
        )
    }

    fun sendEmail(email: String, title: String, content: String, async : Boolean = true) {
        val emailHost = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.EMAIL_HOST)) { "Email host config not found" }
        val emailPort = configCacheService.getValueAsInt(ConfigPropertyKey.EMAIL_PORT)
        require(emailPort != -1) { "Email port config not found" }
        val emailUsername = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.EMAIL_USERNAME)) { "Email username config not found" }
        val emailPassword = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.EMAIL_PASSWORD)) { "Email password config not found" }

        val block: () -> Unit = {
            try {
                sendEmail(
                    emailHost,
                    emailPort,
                    emailUsername,
                    emailPassword,
                    email,
                    title,
                    content
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (async) {
            Thread(block).start()
        } else {
            block()
        }
    }
}