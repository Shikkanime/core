package fr.shikkanime.repositories

import fr.shikkanime.dtos.analytics.KeyCountDto
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.TraceAction_
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.services.TraceActionService
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class TraceActionRepository : AbstractRepository<TraceAction>() {
    override fun getEntityClass() = TraceAction::class.java

    fun findAllBy(entityType: String?, action: String?, page: Int, limit: Int): Pageable<TraceAction> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            val predicates = mutableListOf(cb.conjunction())
            entityType?.let { entityType -> predicates.add(cb.equal(root[TraceAction_.entityType], entityType)) }
            action?.let { action -> predicates.add(cb.equal(root[TraceAction_.action], action)) }
            query.where(*predicates.toTypedArray())

            query.orderBy(cb.desc(root[TraceAction_.actionDateTime]))

            buildPageableQuery(createReadOnlyQuery(it, query), page, limit)
        }
    }

    fun getReturningUserUuids(since: ZonedDateTime, minDays: Int): List<UUID> {
        return database.entityManager.use { em ->
            val cb = em.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())
            val actionDate = cb.function("date", LocalDate::class.java, root[TraceAction_.actionDateTime])

            query.select(root[TraceAction_.entityUuid])
                .where(
                    cb.equal(root[TraceAction_.action], TraceAction.Action.LOGIN),
                    cb.greaterThan(root[TraceAction_.actionDateTime], since)
                )
                .groupBy(root[TraceAction_.entityUuid])
                .having(cb.ge(cb.countDistinct(actionDate), minDays))

            em.createQuery(query).resultList
        }
    }

    fun findLastLoginsByEntityUuids(returningUuids: List<UUID>): List<UUID> {
        if (returningUuids.isEmpty()) return emptyList()

        return database.entityManager.use { em ->
            val cb = em.criteriaBuilder
            val query = cb.createQuery(UUID::class.java)
            val root = query.from(getEntityClass())

            val subquery = query.subquery(ZonedDateTime::class.java)
            val subRoot = subquery.from(getEntityClass())

            subquery.select(cb.function("MAX", ZonedDateTime::class.java, subRoot[TraceAction_.actionDateTime]))
                .where(
                    cb.equal(subRoot[TraceAction_.action], TraceAction.Action.LOGIN),
                    subRoot[TraceAction_.entityUuid].`in`(returningUuids)
                )
                .groupBy(subRoot[TraceAction_.entityUuid])

            query.select(root[TraceAction_.uuid])
                .where(
                    cb.equal(root[TraceAction_.action], TraceAction.Action.LOGIN),
                    root[TraceAction_.actionDateTime].`in`(subquery),
                    root[TraceAction_.entityUuid].`in`(returningUuids)
                )

            createReadOnlyQuery(em, query).resultList
        }
    }

    fun getDailyReturningUserCount(since: ZonedDateTime, returningUuids: List<UUID>): List<TraceActionService.DateVersionCountDto> {
        if (returningUuids.isEmpty()) return emptyList()

        return database.entityManager.use { em ->
            val cb = em.criteriaBuilder
            val query = cb.createQuery(TraceActionService.DateVersionCountDto::class.java)
            val root = query.from(getEntityClass())

            val actionDate = cb.function("date", String::class.java, root[TraceAction_.actionDateTime])
            val appVersion = cb.function("json_extract_path_text", String::class.java, cb.function("json", String::class.java, root[TraceAction_.additionalData]), cb.literal("appVersion"))

            query.select(
                cb.construct(
                    TraceActionService.DateVersionCountDto::class.java,
                    actionDate,
                    appVersion,
                    cb.countDistinct(root[TraceAction_.entityUuid])
                )
            ).where(
                cb.equal(root[TraceAction_.action], TraceAction.Action.LOGIN),
                cb.greaterThan(root[TraceAction_.actionDateTime], since),
                root[TraceAction_.entityUuid].`in`(returningUuids),
                cb.isNotNull(root[TraceAction_.additionalData])
            ).groupBy(actionDate, appVersion)
                .orderBy(cb.asc(actionDate), cb.asc(appVersion))

            em.createQuery(query).resultList
        }
    }

    private fun getCountPerJsonKey(
        since: ZonedDateTime,
        traceActionUuids: List<UUID>,
        jsonKey: String,
        keyTransformer: ((CriteriaBuilder, Expression<String>) -> Expression<String>)? = null
    ): List<KeyCountDto> {
        if (traceActionUuids.isEmpty()) return emptyList()

        return database.entityManager.use { em ->
            val cb = em.criteriaBuilder
            val query = cb.createQuery(KeyCountDto::class.java)
            val root = query.from(getEntityClass())

            var keyExpr = cb.function("json_extract_path_text", String::class.java, cb.function("json", String::class.java, root[TraceAction_.additionalData]), cb.literal(jsonKey))
            keyTransformer?.let { keyExpr = it(cb, keyExpr) }

            query.select(
                cb.construct(
                    KeyCountDto::class.java,
                    keyExpr,
                    cb.countDistinct(root[TraceAction_.entityUuid])
                )
            ).where(
                cb.equal(root[TraceAction_.action], TraceAction.Action.LOGIN),
                cb.greaterThan(root[TraceAction_.actionDateTime], since),
                root[TraceAction_.uuid].`in`(traceActionUuids),
                cb.isNotNull(root[TraceAction_.additionalData])
            ).groupBy(keyExpr)
                .orderBy(cb.asc(keyExpr))

            em.createQuery(query).resultList
        }
    }

    fun getAppVersionCount(since: ZonedDateTime, lastLoginTraceActionUuids: List<UUID>) = getCountPerJsonKey(since, lastLoginTraceActionUuids, "appVersion")

    fun getLocaleCount(since: ZonedDateTime, lastLoginTraceActionUuids: List<UUID>) = getCountPerJsonKey(since, lastLoginTraceActionUuids, "locale") { cb, keyExpr ->
        cb.substring(keyExpr, 1, 2)
    }

    fun getDeviceCount(since: ZonedDateTime, lastLoginTraceActionUuids: List<UUID>) = getCountPerJsonKey(since, lastLoginTraceActionUuids, "device")
}