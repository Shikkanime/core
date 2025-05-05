package fr.shikkanime.repositories

import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberNotificationSettings
import fr.shikkanime.entities.MemberNotificationSettings_
import org.hibernate.Hibernate

class MemberNotificationSettingsRepository : AbstractRepository<MemberNotificationSettings>() {
    override fun getEntityClass() = MemberNotificationSettings::class.java

    fun findByMember(member: Member): MemberNotificationSettings? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())
            query.select(root)

            query.where(
                cb.equal(root[MemberNotificationSettings_.member], member)
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
                .apply { Hibernate.initialize(this?.platforms) }
        }
    }
}