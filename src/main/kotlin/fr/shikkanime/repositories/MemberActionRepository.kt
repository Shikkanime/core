package fr.shikkanime.repositories

import fr.shikkanime.entities.MemberAction
import fr.shikkanime.entities.MemberAction_
import java.util.*

class MemberActionRepository : AbstractRepository<MemberAction>() {
    override fun getEntityClass() = MemberAction::class.java

    fun findByUuidAndCode(uuid: UUID, code: String): MemberAction? {
        val cb = entityManager.criteriaBuilder
        val query = cb.createQuery(getEntityClass())
        val root = query.from(getEntityClass())

        query.where(
            cb.equal(root[MemberAction_.uuid], uuid),
            cb.equal(root[MemberAction_.code], code),
            cb.isFalse(root[MemberAction_.validated])
        )

        return createReadOnlyQuery(entityManager, query)
            .resultList
            .firstOrNull()
    }
}