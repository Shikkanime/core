package fr.shikkanime.repositories

import fr.shikkanime.entities.Member
import fr.shikkanime.entities.enums.Role

class MemberRepository : AbstractRepository<Member>() {
    fun findAllByRole(role: Role): List<Member> {
        return createQuery("FROM Member WHERE role = :role", getEntityClass())
            .setParameter("role", role)
            .resultList
    }

    fun findByUsernameAndPassword(username: String, password: ByteArray): Member? {
        return createQuery("FROM Member WHERE username = :username AND encryptedPassword = :password", getEntityClass())
            .setParameter("username", username)
            .setParameter("password", password)
            .resultList
            .firstOrNull()
    }
}