package fr.shikkanime.repositories

import fr.shikkanime.dtos.analytics.KeyCountDto
import fr.shikkanime.entities.Attachment
import fr.shikkanime.entities.Attachment_
import fr.shikkanime.entities.enums.ImageType
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

class AttachmentRepository : AbstractRepository<Attachment>() {
    suspend fun findAllByEntityUuid(entityUuid: UUID): List<Attachment> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(
                cb.equal(root[Attachment_.entityUuid], entityUuid)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllByEntityUuidAndType(entityUuid: UUID, type: ImageType): List<Attachment> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            query.where(
                cb.equal(root[Attachment_.entityUuid], entityUuid),
                cb.equal(root[Attachment_.type], type)
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllNeededUpdate(lastUpdateDateTime: ZonedDateTime): List<Attachment> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

            val subquery1 = query.subquery(Long::class.java)
            val subroot1 = subquery1.from(entityClass)
            subquery1.select(cb.literal(1L))
            subquery1.where(
                cb.and(
                    cb.equal(subroot1[Attachment_.entityUuid], root[Attachment_.entityUuid]),
                    cb.equal(subroot1[Attachment_.type], root[Attachment_.type]),
                    cb.isTrue(subroot1[Attachment_.active])
                )
            )

            val subquery2 = query.subquery(ZonedDateTime::class.java)
            val subroot2 = subquery2.from(entityClass)
            subquery2.select(cb.greatest(subroot2[Attachment_.creationDateTime]))
            subquery2.where(
                cb.and(
                    cb.equal(subroot2[Attachment_.entityUuid], root[Attachment_.entityUuid]),
                    cb.equal(subroot2[Attachment_.type], root[Attachment_.type])
                )
            )

            query.where(
                cb.or(
                    cb.and(
                        cb.lessThanOrEqualTo(root[Attachment_.lastUpdateDateTime], lastUpdateDateTime),
                        cb.isNotNull(root[Attachment_.url]),
                        cb.isTrue(root[Attachment_.active])
                    ),
                    cb.and(
                        root[Attachment_.type].`in`(listOf(ImageType.THUMBNAIL, ImageType.BANNER)),
                        cb.isNotNull(root[Attachment_.url]),
                        cb.isFalse(root[Attachment_.active]),
                        cb.not(cb.exists(subquery1)),
                        cb.equal(root[Attachment_.creationDateTime], subquery2)
                    )
                )
            )

            createReadOnlyQuery(it, query)
                .resultList
        }
    }

    suspend fun findAllActiveWithUrlAndNotIn(uuids: HashSet<UUID>): List<Attachment> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

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

    suspend fun getCumulativeAttachmentCounts(): List<KeyCountDto> {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createTupleQuery()
            val root = query.from(entityClass)
            val formattedCreationDate = cb.function("date", LocalDate::class.java, root[Attachment_.creationDateTime])

            query.select(cb.tuple(formattedCreationDate, cb.count(root)))
                .where(cb.isTrue(root[Attachment_.active]))
                .groupBy(formattedCreationDate)
                .orderBy(cb.asc(formattedCreationDate))

            var sum = 0L
            createReadOnlyQuery(it, query)
                .resultList
                .map { tuple ->
                    sum += tuple[1, Long::class.java]
                    KeyCountDto(tuple[0, LocalDate::class.java].toString(), sum)
                }
        }
    }

    suspend fun findByEntityUuidTypeAndActive(entityUuid: UUID, type: ImageType): Attachment? {
        return dispatch {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(entityClass)
            val root = query.from(entityClass)

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