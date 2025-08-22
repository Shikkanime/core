package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Mail
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberAction
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.Action
import fr.shikkanime.repositories.MemberActionRepository
import fr.shikkanime.utils.*
import fr.shikkanime.utils.TelemetryConfig.trace
import java.time.ZonedDateTime
import java.util.*
import java.util.logging.Level
import kotlin.io.encoding.ExperimentalEncodingApi

class MemberActionService : AbstractService<MemberAction, MemberActionRepository>() {
    companion object {
        const val ACTION_EXPIRED_AFTER = 15L
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private val tracer = TelemetryConfig.getTracer("MemberActionService")
    @Inject private lateinit var memberActionRepository: MemberActionRepository
    @Inject private lateinit var memberService: MemberService
    @Inject private lateinit var mailService: MailService
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = memberActionRepository

    private fun findByUuidAndCode(uuid: UUID, code: String) = tracer.trace { memberActionRepository.findByUuidAndCode(uuid, code) }

    @OptIn(ExperimentalEncodingApi::class)
    private fun toWebToken(memberAction: MemberAction): String = EncryptionManager.toBase64(EncryptionManager.toSHA512("${memberAction.member?.uuid}|${memberAction.uuid}|${memberAction.action}|${memberAction.code}").toByteArray())

    fun validateWebAction(webToken: String) {
        val actionTokens = memberActionRepository.findAllNotValidated().associateBy(this::toWebToken)
        require(actionTokens.containsKey(webToken)) { "Action not found" }
        val memberAction = actionTokens[webToken] ?: return
        doValidateAction(memberAction)
    }

    fun validateAction(uuid: UUID, code: String) {
        val memberAction = findByUuidAndCode(uuid, code)
        checkNotNull(memberAction) { "Invalid action" }
        require(ZonedDateTime.now() < memberAction.creationDateTime.plusMinutes(ACTION_EXPIRED_AFTER)) { "Action expired" }
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

    private fun doAction(memberAction: MemberAction, code: String, email: String) {
        when (memberAction.action) {
            Action.VALIDATE_EMAIL -> {
                mailService.save(
                    Mail(
                        recipient = email,
                        title = "${Constant.NAME} - Vérification d'adresse email",
                        body = mailService.getFreemarkerContent("/mail/associate-email.ftl", code, model = mapOf("webToken" to toWebToken(memberAction))).toString()
                    )
                )
            }

            Action.FORGOT_IDENTIFIER -> {
                mailService.save(
                    Mail(
                        recipient = email,
                        title = "${Constant.NAME} - Récupération d'identifiant",
                        body = mailService.getFreemarkerContent("/mail/forgot-identifier.ftl", code).toString()
                    )
                )
            }

            else -> TODO("Action not implemented")
        }
    }

    private fun doValidateAction(memberAction: MemberAction) {
        tracer.trace {
            when (memberAction.action) {
                Action.VALIDATE_EMAIL -> {
                    memberAction.member!!.email = memberAction.email
                    memberService.update(memberAction.member!!)
                    traceActionService.createTraceAction(memberAction.member!!, TraceAction.Action.UPDATE)

                    mailService.save(
                        Mail(
                            recipient = memberAction.email!!,
                            title = "${Constant.NAME} - Adresse email validée",
                            body = mailService.getFreemarkerContent("/mail/email-associated.ftl").toString()
                        )
                    )
                }

                Action.FORGOT_IDENTIFIER -> {
                    var identifier: String

                    do {
                        identifier = StringUtils.generateRandomString(12)
                    } while (memberService.findByIdentifier(identifier) != null)

                    memberAction.member!!.username = EncryptionManager.toSHA512(identifier)
                    memberService.update(memberAction.member!!)
                    traceActionService.createTraceAction(memberAction.member!!, TraceAction.Action.UPDATE)

                    mailService.save(
                        Mail(
                            recipient = memberAction.email!!,
                            title = "${Constant.NAME} - Votre nouvel identifiant",
                            body = mailService.getFreemarkerContent("/mail/your-new-identifier.ftl", identifier).toString()
                        )
                    )
                }

                else -> TODO("Action not implemented")
            }

            memberAction.validated = true
            memberAction.updateDateTime = ZonedDateTime.now()
            memberActionRepository.update(memberAction)
        }
    }
}