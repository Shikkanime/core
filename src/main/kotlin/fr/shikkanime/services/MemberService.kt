package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.enums.Action
import fr.shikkanime.entities.enums.Role
import fr.shikkanime.repositories.MemberRepository
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.RandomManager
import java.util.*

class MemberService : AbstractService<Member, MemberRepository>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var memberRepository: MemberRepository

    @Inject
    private lateinit var memberActionService: MemberActionService

    override fun getRepository() = memberRepository

    private fun findAllByRoles(roles: List<Role>) = memberRepository.findAllByRoles(roles)

    fun findAllByAnimeUUID(animeUuid: UUID) = memberRepository.findAllByAnimeUUID(animeUuid)

    fun findByUsernameAndPassword(username: String, password: String) =
        memberRepository.findByUsernameAndPassword(username, EncryptionManager.generate(password))

    fun findByIdentifier(identifier: String) =
        memberRepository.findByIdentifier(EncryptionManager.toSHA512(identifier))

    fun findByEmail(email: String) = memberRepository.findByEmail(email)

    fun initDefaultAdminUser(): String {
        val adminUsers = findAllByRoles(listOf(Role.ADMIN))
        check(adminUsers.isEmpty()) { "Admin user already exists" }
        val password = RandomManager.generateRandomString(32)
        logger.info("Default admin password: $password")
        save(
            Member(
                username = "admin",
                encryptedPassword = EncryptionManager.generate(password),
                roles = mutableSetOf(Role.ADMIN)
            )
        )
        return password
    }

    fun associateEmail(memberUuid: UUID, email: String): UUID {
        val member = requireNotNull(find(memberUuid))
        // Creation member action
        return memberActionService.save(Action.VALIDATE_EMAIL, member, email)
    }

    fun forgotIdentifier(member: Member): UUID {
        requireNotNull(member.email)
        // Creation member action
        return memberActionService.save(Action.FORGOT_IDENTIFIER, member, member.email!!)
    }

    fun save(identifier: String) =
        save(
            Member(
                isPrivate = true,
                username = EncryptionManager.toSHA512(identifier),
                encryptedPassword = byteArrayOf()
            )
        )
}