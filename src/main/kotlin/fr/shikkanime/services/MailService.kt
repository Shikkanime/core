package fr.shikkanime.services

import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import java.util.*

object MailService {
    fun sendEmail(
        emailHost: String,
        emailPort: Int,
        emailUsername: String,
        emailPassword: String,
        email: String,
        title: String,
        body: String
    ) {
        val properties = Properties()
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"
        properties["mail.smtp.host"] = emailHost
        properties["mail.smtp.port"] = emailPort.toString()
        properties["mail.smtp.ssl.trust"] = emailHost

        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(emailUsername, emailPassword)
            }
        })

        val message = MimeMessage(session)
        message.setFrom(InternetAddress(emailUsername, "Shikkanime - Ne pas répondre"))
        message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(email))
        message.subject = title

        val mimeBodyPart = MimeBodyPart()

        mimeBodyPart.setContent(body, "text/html; charset=utf-8")

        val multipart = MimeMultipart("related")
        multipart.addBodyPart(mimeBodyPart)
        message.setContent(multipart)
        Transport.send(message)
    }
}