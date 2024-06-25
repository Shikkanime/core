package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.Role
import java.util.*

class MemberRepository : AbstractRepository<Member>() {
    override fun getEntityClass() = Member::class.java

    fun findAllByRoles(roles: List<Role>): List<Member> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())
        query.where(root.join(Member_.roles).`in`(roles))

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findAllByAnimeUUID(animeUuid: UUID): List<Member> {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(MemberFollowAnime::class.java)

        query.select(root[MemberFollowAnime_.member])
        query.distinct(true)
        query.where(cb.equal(root[MemberFollowAnime_.anime][Anime_.uuid], animeUuid))

        return createReadOnlyQuery(entityManager, query)
            .resultList
    }

    fun findByUsernameAndPassword(username: String, password: ByteArray): Member? {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        query.where(
            cb.equal(root[Member_.username], username),
            cb.equal(root[Member_.encryptedPassword], password)
        )

        return createReadOnlyQuery(entityManager, query)
            .resultList
            .firstOrNull()
    }

    fun findByIdentifier(identifier: String): Member? {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())
        query.where(cb.equal(root[Member_.username], identifier))

        return createReadOnlyQuery(entityManager, query)
            .resultList
            .firstOrNull()
    }

    fun findByEmail(email: String): Member? {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())
        query.where(cb.equal(root[Member_.email], email))

        return createReadOnlyQuery(entityManager, query)
            .resultList
            .firstOrNull()
    }
}