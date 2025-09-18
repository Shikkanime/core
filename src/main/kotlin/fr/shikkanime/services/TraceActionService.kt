package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.analytics.KeyCountDto
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.repositories.TraceActionRepository
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
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
        val dailyUserVersionCounts: List<DateVersionCountDto>
    )

    @Inject
    private lateinit var traceActionRepository: TraceActionRepository

    override fun getRepository() = traceActionRepository

    fun findAllBy(entityType: String?, action: String?, page: Int, limit: Int) = traceActionRepository.findAllBy(entityType, action, page, limit)

    fun getAnalyticsTraceActions(date: LocalDate, minActiveDays: Int = 2): AnalyticsReport {
        val parsedActions = traceActionRepository.getAnalyticsTraceActions(date)
            .mapNotNull { action -> action.additionalData?.let { data -> action to ObjectParser.fromJson(data) } }

        val loginsByReturningUser = parsedActions
            .groupBy { (action, _) -> action.entityUuid!! }
            .filter { (_, userActions) -> userActions.map { (action, _) -> action.actionDateTime!!.toLocalDate() }.distinct().size >= minActiveDays }

        val lastLoginsOfReturningUsers = loginsByReturningUser.values.map { actions -> actions.maxByOrNull { (action, _) -> action.actionDateTime!! }!! }

        val versionCounts = lastLoginsOfReturningUsers
            .groupingBy { (_, data) -> data.getAsString("appVersion")!! }
            .eachCount()
            .map { (version, count) -> KeyCountDto(version, count.toLong()) }
            .sortedBy { it.key }

        val localeCounts = lastLoginsOfReturningUsers
            .groupingBy { (_, data) -> data.getAsString("locale")!!.substring(0, 2) }
            .eachCount()
            .map { (locale, count) -> KeyCountDto(locale, count.toLong()) }
            .sortedByDescending { it.count }

        val deviceCounts = lastLoginsOfReturningUsers
            .groupingBy { (_, data) -> data.getAsString("device")!!.lowercase() }
            .eachCount()
            .map { (device, count) -> KeyCountDto(device, count.toLong()) }
            .sortedByDescending { it.count }

        val allLoginsFromReturningUsers = loginsByReturningUser.values.flatten()
        val dailyUserVersionCounts = allLoginsFromReturningUsers
            .groupBy { (action, data) -> action.actionDateTime!!.toLocalDate() to data.getAsString("appVersion")!! }
            .mapValues { (_, actions) -> actions.map { (action, _) -> action.entityUuid!! }.distinct().size.toLong() }
            .map { (pair, count) -> DateVersionCountDto(pair.first.toString(), pair.second, count) }
            .sortedWith(compareBy({ it.date }, { it.version }))

        return AnalyticsReport(versionCounts, localeCounts, deviceCounts, dailyUserVersionCounts)
    }

    fun createTraceAction(shikkEntity: ShikkEntity, action: TraceAction.Action, additionalData: String? = null) = save(
        TraceAction(
            actionDateTime = ZonedDateTime.now(),
            entityType = shikkEntity::class.java.simpleName,
            entityUuid = shikkEntity.uuid,
            action = action,
            additionalData = additionalData
        )
    )
}