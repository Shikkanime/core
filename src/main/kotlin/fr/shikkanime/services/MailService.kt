package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Mail
import fr.shikkanime.repositories.MailRepository
import fr.shikkanime.utils.Constant
import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import java.io.StringWriter

class MailService : AbstractService<Mail, MailRepository>() {
    @Inject
    private lateinit var mailRepository: MailRepository

    override fun getRepository() = mailRepository

    fun findAllNotSent() = mailRepository.findAllNotSent()

    fun getFreemarkerContent(template: String, code: String? = null, model: Map<String, String>? = null): StringWriter {
        val stringWriter = StringWriter()
        val configuration = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
        configuration.templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        configuration.defaultEncoding = "UTF-8"

        configuration.getTemplate(template).process(
            mutableMapOf(
                "baseUrl" to Constant.baseUrl,
                "code" to code,
            ).apply {
                model?.let(this::putAll)
            }, stringWriter
        )

        return stringWriter
    }
}