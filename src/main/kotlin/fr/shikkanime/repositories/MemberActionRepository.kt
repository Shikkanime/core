package fr.shikkanime.repositories

import fr.shikkanime.entities.MemberAction
import fr.shikkanime.entities.MemberAction_
import fr.shikkanime.entities.Member_
import fr.shikkanime.services.MemberActionService
import java.time.ZonedDateTime
import java.util.*

class MemberActionRepository : AbstractRepository<MemberAction>() {
    suspend fun findAllByMember(memberUuid: UUID): List<MemberAction> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(
                cb.equal(root[MemberAction_.member][Member_.uuid], memberUuid)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllNotValidated(): List<MemberAction> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(
                cb.isFalse(root[MemberAction_.validated]),
                cb.greaterThan(root[MemberAction_.creationDateTime], ZonedDateTime.now().minusMinutes(MemberActionService.ACTION_EXPIRED_AFTER))
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findByUuidAndCode(uuid: UUID, code: String): MemberAction? {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

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