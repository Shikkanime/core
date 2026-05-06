package fr.shikkanime.repositories

import fr.shikkanime.entities.Mail
import fr.shikkanime.entities.Mail_

class MailRepository : AbstractRepository<Mail>() {
    suspend fun findAllByRecipient(recipient: String): List<Mail> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[Mail_.recipient], recipient)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllNotSent(): List<Mail> {
        return dispatch {
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