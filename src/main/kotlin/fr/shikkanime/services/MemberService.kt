package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.enums.Role
import fr.shikkanime.repositories.MemberRepository
import fr.shikkanime.utils.EncryptionManager
import fr.shikkanime.utils.RandomManager


class MemberService : AbstractService<Member, MemberRepository>() {
    @Inject
    private lateinit var memberRepository: MemberRepository

    override fun getRepository(): MemberRepository {
        return memberRepository
    }

    private fun findByRole(role: Role): List<Member> {
        return memberRepository.findByRole(role)
    }

    fun findByUsernameAndPassword(username: String, password: String): Member? {
        return memberRepository.findByUsernameAndPassword(username, EncryptionManager.generate(password))
    }

    fun initDefaultAdminUser() {
        val adminUsers = findByRole(Role.ADMIN)

        if (adminUsers.isNotEmpty()) {
            return
        }

        val password = RandomManager.generateRandomString(32)
        println("Default admin password: $password")
        save(Member(username = "admin", encryptedPassword = EncryptionManager.generate(password), role = Role.ADMIN))
    }
}