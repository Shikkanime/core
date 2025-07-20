package fr.shikkanime.repositories

import fr.shikkanime.entities.Mail
import fr.shikkanime.entities.Mail_

class MailRepository : AbstractRepository<Mail>() {
    override fun getEntityClass() = Mail::class.java

    fun findAllNotSent(): List<Mail> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.isFalse(root[Mail_.sent]),
                    cb.isNull(root[Mail_.error])
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }
}