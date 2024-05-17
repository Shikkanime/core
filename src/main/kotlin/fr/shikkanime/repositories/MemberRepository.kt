package fr.shikkanime.repositories

import fr.shikkanime.entities.Member
import fr.shikkanime.entities.Member_
import fr.shikkanime.entities.enums.Role

class MemberRepository : AbstractRepository<Member>() {
    override fun getEntityClass() = Member::class.java

    fun findAllByRoles(roles: List<Role>): List<Member> {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(root.join(Member_.roles).`in`(roles))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findByUsernameAndPassword(username: String, password: ByteArray): Member? {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[Member_.username], username),
                cb.equal(root[Member_.encryptedPassword], password)
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }

    fun findByIdentifier(identifier: String): Member? {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(cb.equal(root[Member_.username], identifier))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }

    fun findByEmail(email: String): Member? {
        return inTransaction {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.where(cb.equal(root[Member_.email], email))

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}