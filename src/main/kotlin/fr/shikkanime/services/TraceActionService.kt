package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.analytics.KeyCountDto
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.TraceActionRepository
import fr.shikkanime.utils.Constant
import java.time.LocalDate
import java.time.ZonedDateTime

class TraceActionService : AbstractService<TraceAction, TraceActionRepository>() {
    data class DateVersionCountDto(
        val date: String,
        val version: String,
        val count: Long
    )

    data class AnalyticsReport(
        val versionCounts: List<KeyCountDto>,
        val localeCounts: List<KeyCountDto>,
        val deviceCounts: List<KeyCountDto>,
        val dailyUserVersionCounts: List<DateVersionCountDto>,
        val dailyWmaCounts: List<KeyCountDto>
    )

    @Inject private lateinit var traceActionRepository: TraceActionRepository

    override fun getRepository() = traceActionRepository

    fun findAllBy(entityType: String?, action: String?, page: Int, limit: Int) = traceActionRepository.findAllBy(entityType, action, page, limit)

    fun getUserAnalytics(date: LocalDate, minActiveDays: Int = 2): AnalyticsReport {
        val since = date.atStartOfDay(Constant.utcZoneId)
        val uuids = traceActionRepository.getReturningUserUuids(since, minActiveDays)
        val lastLoginUuids = traceActionRepository.findLastLoginsByEntityUuids(uuids)
        val versionCounts = traceActionRepository.getAppVersionCount(since, lastLoginUuids)
        val localeCounts = traceActionRepository.getLocaleCount(since, lastLoginUuids)
        val deviceCounts = traceActionRepository.getDeviceCount(since, lastLoginUuids)
        val dailyUserVersionCounts = traceActionRepository.getDailyReturningUserCount(since, uuids)
        val dailyWmaCounts = traceActionRepository.getDailyWmaCount(since, uuids)
        return AnalyticsReport(versionCounts, localeCounts, deviceCounts, dailyUserVersionCounts, dailyWmaCounts)
    }

    fun createTraceAction(entity: ShikkEntity, action: TraceAction.Action, additionalData: String? = null) = save(
        TraceAction(
            actionDateTime = ZonedDateTime.now(),
            entityType = entity::class.java.simpleName,
            entityUuid = entity.uuid,
            action = action,
            additionalData = additionalData
        )
    )

    fun createTraceActions(entities: List<ShikkEntity>, action: TraceAction.Action) {
        val now = ZonedDateTime.now()

        saveAll(
            entities.map {
                TraceAction(
                    actionDateTime = now,
                    entityType = it::class.java.simpleName,
                    entityUuid = it.uuid,
                    action = action,
                )
            }
        )
    }
}