package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.MailService
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.LoggerFactory
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import java.time.ZonedDateTime

class SendMailJob : AbstractJob {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var mailService: MailService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun run() {
        val mails = mailService.findAllNotSent()

        if (mails.isEmpty())
            return

        val emailHost = configCacheService.getValueAsString(ConfigPropertyKey.EMAIL_HOST)
        val emailPort = configCacheService.getValueAsInt(ConfigPropertyKey.EMAIL_PORT)
        val emailUsername = configCacheService.getValueAsString(ConfigPropertyKey.EMAIL_USERNAME)
        val emailPassword = configCacheService.getValueAsString(ConfigPropertyKey.EMAIL_PASSWORD)

        val mailer = MailerBuilder.withSMTPServer(emailHost, emailPort, emailUsername, emailPassword)
            .withTransportStrategy(TransportStrategy.SMTP_TLS)
            .withConnectionPoolCoreSize(10)
            .buildMailer()

        try {
            mailer.testConnection()
        } catch (e: Exception) {
            logger.warning("Failed to connect to email server: ${e.message}")
            mailer.close()
            return
        }

        logger.info("Sending ${mails.size} emails...")

        mails.forEach { mail ->
            try {
                mailer.sendMail(
                    EmailBuilder.startingBlank()
                        .from("${Constant.NAME} - Ne pas répondre", emailUsername!!)
                        .to(mail.recipient!!)
                        .withSubject(mail.title!!)
                        .withHTMLText(mail.body!!)
                        .buildEmail()
                )

                mail.sent = true
                mail.lastUpdateDateTime = ZonedDateTime.now()
                mailService.update(mail)
            } catch (e: Exception) {
                logger.warning("Failed to send email: ${e.message}")

                mail.error = e.stackTraceToString()
                mail.lastUpdateDateTime = ZonedDateTime.now()
                mailService.update(mail)
            }
        }

        mailer.close()
    }
}