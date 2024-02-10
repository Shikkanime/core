package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.enums.Role
import fr.shikkanime.repositories.MemberRepository
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.LoggerFactory
import fr.shikkanime.utils.RandomManager


class MemberService : AbstractService<Member, MemberRepository>() {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Inject
    private lateinit var memberRepository: MemberRepository

    override fun getRepository() = memberRepository

    private fun findAllByRoles(roles: List<Role>) = memberRepository.findAllByRoles(roles)

    fun findByUsernameAndPassword(username: String, password: String) =
        memberRepository.findByUsernameAndPassword(username, EncryptionManager.generate(password))

    fun initDefaultAdminUser(): String {
        val adminUsers = findAllByRoles(listOf(Role.ADMIN))
        check(adminUsers.isEmpty()) { "Admin user already exists" }
        val password = RandomManager.generateRandomString(32)
        logger.info("Default admin password: $password")
        save(Member(username = "admin", encryptedPassword = EncryptionManager.generate(password), roles = mutableSetOf(Role.ADMIN)))
        return password
    }
}