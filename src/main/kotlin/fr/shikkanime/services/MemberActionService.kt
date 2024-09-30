package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberAction
import fr.shikkanime.entities.enums.Action
import fr.shikkanime.repositories.MemberActionRepository
import fr.shikkanime.utils.*
import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import java.io.StringWriter
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Level

class MemberActionService : AbstractService<MemberAction, MemberActionRepository>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var memberActionRepository: MemberActionRepository

    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var emailService: EmailService

    override fun getRepository() = memberActionRepository

    fun validateAction(uuid: UUID, code: String) {
        val memberAction = memberActionRepository.findByUuidAndCode(uuid, code)
        checkNotNull(memberAction) { "Invalid action" }
        require(ZonedDateTime.now().isBefore(memberAction.creationDateTime.plusMinutes(15))) { "Action expired" }

        try {
            when (memberAction.action) {
                Action.VALIDATE_EMAIL -> {
                    memberAction.member!!.email = memberAction.email
                    memberService.update(memberAction.member!!)
                    MapCache.invalidate(Member::class.java)

                    emailService.sendEmail(
                        memberAction.email!!,
                        "${Constant.NAME} - Adresse email validée",
                        getFreemarkerContent("/mail/email-associated.ftl").toString()
                    )
                }

                Action.FORGOT_IDENTIFIER -> {
                    var identifier: String

                    do {
                        identifier = StringUtils.generateRandomString(12)
                    } while (memberService.findByIdentifier(identifier) != null)

                    memberAction.member!!.username = EncryptionManager.toSHA512(identifier)
                    memberService.update(memberAction.member!!)
                    MapCache.invalidate(Member::class.java)

                    emailService.sendEmail(
                        memberAction.email!!,
                        "${Constant.NAME} - Votre nouvel identifiant",
                        getFreemarkerContent("/mail/your-new-identifier.ftl", identifier).toString()
                    )
                }

                else -> TODO("Action not implemented")
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to validate action", e)
        }

        memberAction.validated = true
        memberAction.updateDateTime = ZonedDateTime.now()
        memberActionRepository.update(memberAction)
    }

    fun save(action: Action, member: Member, email: String): UUID {
        val code = StringUtils.generateRandomString(8).uppercase()

        val savedAction = save(
            MemberAction(
                member = member,
                email = email,
                action = action,
                code = code
            )
        )

        try {
            doAction(action, code, email)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to do action", e)
        }

        logger.info("Action saved with code $code: ${savedAction.uuid}")
        return savedAction.uuid!!
    }

    private fun getFreemarkerContent(template: String, code: String? = null): StringWriter {
        val stringWriter = StringWriter()
        val configuration = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
        configuration.templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        configuration.defaultEncoding = "UTF-8"

        configuration.getTemplate(template).process(
            mapOf(
                "baseUrl" to Constant.baseUrl,
                "code" to code,
            ), stringWriter
        )

        return stringWriter
    }

    private fun doAction(action: Action, code: String, email: String) {
        when (action) {
            Action.VALIDATE_EMAIL -> {
                val stringWriter = getFreemarkerContent("/mail/associate-email.ftl", code)
                emailService.sendEmail(email, "${Constant.NAME} - Vérification d'adresse email", stringWriter.toString())
            }

            Action.FORGOT_IDENTIFIER -> {
                val stringWriter = getFreemarkerContent("/mail/forgot-identifier.ftl", code)
                emailService.sendEmail(email, "${Constant.NAME} - Récupération d'identifiant", stringWriter.toString())
            }
        }
    }
}