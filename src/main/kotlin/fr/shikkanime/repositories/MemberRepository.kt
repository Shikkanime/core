package fr.shikkanime.repositories

import fr.shikkanime.entities.Member
import fr.shikkanime.entities.enums.Role
import org.hibernate.Hibernate
import java.util.*

class MemberRepository : AbstractRepository<Member>() {
    override fun getEntityClass() = Member::class.java

    private fun Member.initialize(): Member {
        if (!Hibernate.isInitialized(this.roles)) {
            Hibernate.initialize(this.roles)
        }

        return this
    }

    private fun List<Member>.initialize(): List<Member> {
        this.forEach { member -> member.initialize() }
        return this
    }

    override fun find(uuid: UUID) = inTransaction {
        it.find(getEntityClass(), uuid)?.initialize()
    }

    fun findAllByRoles(roles: List<Role>): List<Member> {
        return inTransaction {
            createReadOnlyQuery(it, "SELECT m FROM Member m JOIN m.roles r WHERE r IN :roles", getEntityClass())
                .setParameter("roles", roles)
                .resultList
                .initialize()
        }
    }

    fun findByUsernameAndPassword(username: String, password: ByteArray): Member? {
        return inTransaction {
            createReadOnlyQuery(
                it,
                "FROM Member WHERE username = :username AND encryptedPassword = :password",
                getEntityClass()
            )
                .setParameter("username", username)
                .setParameter("password", password)
                .resultList
                .firstOrNull()
                ?.initialize()
        }
    }
}