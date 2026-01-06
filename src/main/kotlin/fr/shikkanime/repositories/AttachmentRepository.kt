package fr.shikkanime.repositories

import fr.shikkanime.dtos.analytics.KeyCountDto
import fr.shikkanime.entities.Attachment
import fr.shikkanime.entities.Attachment_
import fr.shikkanime.entities.enums.ImageType
import java.time.LocalDate
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
                    cb.notEqual(root[Attachment_.url], ""),
                    root[Attachment_.uuid].`in`(uuids).not()
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    fun getCumulativeAttachmentCounts(): List<KeyCountDto> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(getEntityClass())
            val formattedCreationDate = cb.function("date", LocalDate::class.java, root[Attachment_.creationDateTime])

            query.select(cb.tuple(formattedCreationDate, cb.count(root)))
                .where(cb.isTrue(root[Attachment_.active]))
                .groupBy(formattedCreationDate)
                .orderBy(cb.asc(formattedCreationDate))

            var sum = 0L
            createReadOnlyQuery(it, query)
                .resultList
                .map {
                    sum += it[1, Long::class.java]
                    KeyCountDto(it[0, LocalDate::class.java].toString(), sum)
                }
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