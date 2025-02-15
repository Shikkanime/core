package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Mail
import fr.shikkanime.repositories.MailRepository

class MailService : AbstractService<Mail, MailRepository>() {
    @Inject
    private lateinit var mailRepository: MailRepository

    override fun getRepository() = mailRepository

    fun findAllNotSent() = mailRepository.findAllNotSent()
}