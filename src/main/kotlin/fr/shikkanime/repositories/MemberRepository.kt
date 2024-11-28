package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.Role
import jakarta.persistence.metamodel.SingularAttribute
import java.util.*

class MemberRepository : AbstractRepository<Member>() {
    override fun getEntityClass() = Member::class.java

    fun findAllByRoles(roles: List<Role>): List<Member> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(root.join(Member_.roles).`in`(roles))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllByAnimeUUID(animeUuid: UUID): List<Member> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(MemberFollowAnime::class.java)

            query.select(root[MemberFollowAnime_.member])
            query.distinct(true)
            query.where(cb.equal(root[MemberFollowAnime_.anime][Anime_.uuid], animeUuid))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    private fun findBy(vararg pairs: Pair<SingularAttribute<Member, *>, Any>): Member? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(*pairs.map { pair ->
                cb.equal(root[pair.first], pair.second)
            }.toTypedArray())

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }

    fun findByUsernameAndPassword(username: String, password: ByteArray) = findBy(
        Member_.username to username,
        Member_.encryptedPassword to password
    )

    fun findByIdentifier(identifier: String) = findBy(Member_.username to identifier)

    fun findByEmail(email: String) = findBy(Member_.email to email)
}