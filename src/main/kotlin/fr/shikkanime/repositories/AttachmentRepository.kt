package fr.shikkanime.repositories

import fr.shikkanime.entities.Attachment
import fr.shikkanime.entities.Attachment_
import fr.shikkanime.entities.enums.ImageType
import java.time.ZonedDateTime
import java.util.*

class AttachmentRepository : AbstractRepository<Attachment>() {
    override fun getEntityClass() = Attachment::class.java

    fun findAllByEntityUuidAndType(entityUuid: UUID, type: ImageType): List<Attachment> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[Attachment_.entityUuid], entityUuid),
                cb.equal(root[Attachment_.type], type)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllNeededUpdate(lastUpdateDateTime: ZonedDateTime): List<Attachment> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.lessThanOrEqualTo(root[Attachment_.lastUpdateDateTime], lastUpdateDateTime),
                    cb.isNotNull(root[Attachment_.url]),
                    cb.isTrue(root[Attachment_.active])
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllActiveWithUrlAndNotIn(uuids: HashSet<UUID>): List<Attachment> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.and(
                    cb.isTrue(root[Attachment_.active]),
                    cb.isNotNull(root[Attachment_.url]),
                    root[Attachment_.uuid].`in`(uuids).not()
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findAllActive(): List<Attachment> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(cb.isTrue(root[Attachment_.active]))

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun findByEntityUuidTypeAndActive(entityUuid: UUID, type: ImageType): Attachment? {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[Attachment_.entityUuid], entityUuid),
                cb.equal(root[Attachment_.type], type),
                cb.isTrue(root[Attachment_.active])
            )

            createReadOnlyQuery(it, query)
                .resultList
                .firstOrNull()
        }
    }
}