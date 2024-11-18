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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class MemberActionService : AbstractService<MemberAction, MemberActionRepository>() {
    companion object {
        const val ACTION_EXPIRED_AFTER = 15L
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var memberActionRepository: MemberActionRepository

    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var emailService: EmailService

    override fun getRepository() = memberActionRepository

    @OptIn(ExperimentalEncodingApi::class)
    private fun toWebToken(memberAction: MemberAction): String = Base64.encode(EncryptionManager.toSHA512("${memberAction.member?.uuid}|${memberAction.uuid}|${memberAction.action}|${memberAction.code}").toByteArray())

    fun validateWebAction(webToken: String) {
        val actionTokens = memberActionRepository.findAllNotValidated().associateBy(this::toWebToken)
        require(actionTokens.containsKey(webToken)) { "Action not found" }
        val memberAction = actionTokens[webToken] ?: return
        doValidateAction(memberAction)
    }

    fun validateAction(uuid: UUID, code: String) {
        val memberAction = memberActionRepository.findByUuidAndCode(uuid, code)
        checkNotNull(memberAction) { "Invalid action" }
        require(ZonedDateTime.now().isBefore(memberAction.creationDateTime.plusMinutes(ACTION_EXPIRED_AFTER))) { "Action expired" }
        doValidateAction(memberAction)
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
            doAction(savedAction, code, email)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to do action", e)
        }

        logger.info("Action saved with code $code: ${savedAction.uuid}")
        return savedAction.uuid!!
    }

    private fun getFreemarkerContent(template: String, code: String? = null, model: Map<String, String>? = null): StringWriter {
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

    private fun doAction(memberAction: MemberAction, code: String, email: String) {
        when (memberAction.action) {
            Action.VALIDATE_EMAIL -> {
                val stringWriter = getFreemarkerContent("/mail/associate-email.ftl", code, model = mapOf("webToken" to toWebToken(memberAction)))
                emailService.sendEmail(email, "${Constant.NAME} - Vérification d'adresse email", stringWriter.toString())
            }

            Action.FORGOT_IDENTIFIER -> {
                val stringWriter = getFreemarkerContent("/mail/forgot-identifier.ftl", code)
                emailService.sendEmail(email, "${Constant.NAME} - Récupération d'identifiant", stringWriter.toString())
            }

            else -> TODO("Action not implemented")
        }
    }

    private fun doValidateAction(memberAction: MemberAction) {
        when (memberAction.action) {
            Action.VALIDATE_EMAIL -> {
                memberAction.member!!.email = memberAction.email
                memberService.update(memberAction.member!!)
                MapCache.invalidate(Member::class.java)

                try {
                    emailService.sendEmail(
                        memberAction.email!!,
                        "${Constant.NAME} - Adresse email validée",
                        getFreemarkerContent("/mail/email-associated.ftl").toString()
                    )
                } catch (e: Exception) {
                    logger.log(Level.WARNING, "Failed to send email", e)
                }
            }

            Action.FORGOT_IDENTIFIER -> {
                var identifier: String

                do {
                    identifier = StringUtils.generateRandomString(12)
                } while (memberService.findByIdentifier(identifier) != null)

                memberAction.member!!.username = EncryptionManager.toSHA512(identifier)
                memberService.update(memberAction.member!!)
                MapCache.invalidate(Member::class.java)

                try {
                    emailService.sendEmail(
                        memberAction.email!!,
                        "${Constant.NAME} - Votre nouvel identifiant",
                        getFreemarkerContent("/mail/your-new-identifier.ftl", identifier).toString()
                    )
                } catch (e: Exception) {
                    logger.log(Level.WARNING, "Failed to send email", e)
                }
            }

            else -> TODO("Action not implemented")
        }

        memberAction.validated = true
        memberAction.updateDateTime = ZonedDateTime.now()
        memberActionRepository.update(memberAction)
    }
}