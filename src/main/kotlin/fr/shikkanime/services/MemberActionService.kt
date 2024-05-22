package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberAction
import fr.shikkanime.entities.enums.Action
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.repositories.MemberActionRepository
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import java.io.StringWriter
import java.time.ZonedDateTime
import java.util.*

class MemberActionService : AbstractService<MemberAction, MemberActionRepository>() {
    @Inject
    private lateinit var memberActionRepository: MemberActionRepository

    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    override fun getRepository() = memberActionRepository

    fun validateAction(uuid: UUID, code: String) {
        val memberAction = memberActionRepository.findByUuidAndCode(uuid, code)
        checkNotNull(memberAction) { "Invalid action" }
        require(ZonedDateTime.now().isBefore(memberAction.creationDateTime.plusMinutes(15))) { "Action expired" }

        when (memberAction.action) {
            Action.VALIDATE_EMAIL -> {
                memberAction.member!!.email = memberAction.email
                memberService.update(memberAction.member!!)
                MapCache.invalidate(Member::class.java)
            }
            else -> TODO("Action not implemented")
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

        when (action) {
            Action.VALIDATE_EMAIL -> {
                val stringWriter = StringWriter()
                val configuration = Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
                configuration.templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
                configuration.defaultEncoding = "UTF-8"
                configuration.getTemplate("/mail/associate-email.ftl").process(mapOf("code" to code), stringWriter)

                Thread {
                    try {
                        val emailHost = requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.EMAIL_HOST)) { "Email host config not found" }
                        val emailPort = configCacheService.getValueAsInt(ConfigPropertyKey.EMAIL_PORT)
                        require(emailPort != -1) { "Email port config not found" }
                        val emailUsername =
                            requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.EMAIL_USERNAME)) { "Email username config not found" }
                        val emailPassword =
                            requireNotNull(configCacheService.getValueAsString(ConfigPropertyKey.EMAIL_PASSWORD)) { "Email password config not found" }

                        MailService.sendEmail(
                            emailHost,
                            emailPort,
                            emailUsername,
                            emailPassword,
                            email,
                            "Shikkanime - Vérification d'adresse email",
                            stringWriter.toString()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
            }
        }

        return savedAction.uuid!!
    }
}