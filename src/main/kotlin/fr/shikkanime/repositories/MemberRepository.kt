package fr.shikkanime.repositories

import fr.shikkanime.entities.Member
import fr.shikkanime.entities.enums.Role
import org.hibernate.jpa.AvailableHints

class MemberRepository : AbstractRepository<Member>() {
    fun findAllByRole(role: Role): List<Member> {
        return inTransaction {
            it.createQuery("FROM Member WHERE role = :role", getEntityClass())
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setParameter("role", role)
                .resultList
        }
    }

    fun findByUsernameAndPassword(username: String, password: ByteArray): Member? {
        return inTransaction {
            it.createQuery("FROM Member WHERE username = :username AND encryptedPassword = :password", getEntityClass())
                .setHint(AvailableHints.HINT_READ_ONLY, true)
                .setParameter("username", username)
                .setParameter("password", password)
                .resultList
                .firstOrNull()
        }
    }
}