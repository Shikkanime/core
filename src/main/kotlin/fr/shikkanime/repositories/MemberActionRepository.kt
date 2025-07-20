package fr.shikkanime.repositories

import fr.shikkanime.entities.MemberAction
import fr.shikkanime.entities.MemberAction_
import fr.shikkanime.services.MemberActionService
import java.time.ZonedDateTime
import java.util.*

class MemberActionRepository : AbstractRepository<MemberAction>() {
    override fun getEntityClass() = MemberAction::class.java

    fun findAllNotValidated(): List<MemberAction> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.isFalse(root[MemberAction_.validated]),
                cb.greaterThan(root[MemberAction_.creationDateTime], ZonedDateTime.now().minusMinutes(MemberActionService.ACTION_EXPIRED_AFTER))
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findByUuidAndCode(uuid: UUID, code: String): MemberAction? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[MemberAction_.uuid], uuid),
                cb.equal(root[MemberAction_.code], code),
                cb.isFalse(root[MemberAction_.validated])
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}