package fr.shikkanime.repositories

import fr.shikkanime.entities.MemberAction
import fr.shikkanime.entities.MemberAction_
import java.util.UUID

class MemberActionRepository : AbstractRepository<MemberAction>() {
    override fun getEntityClass() = MemberAction::class.java

    fun findByUuidAndCode(uuid: UUID, code: String): MemberAction? {
        return inTransaction {
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