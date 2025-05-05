package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.dtos.MemberNotificationSettingsDto
import fr.shikkanime.entities.Member
import fr.shikkanime.entities.MemberNotificationSettings
import fr.shikkanime.entities.TraceAction
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.impl.MemberNotificationSettingsFactory
import fr.shikkanime.repositories.MemberNotificationSettingsRepository
import fr.shikkanime.services.caches.MemberCacheService
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.Response
import java.time.ZonedDateTime
import java.util.*

class MemberNotificationSettingsService : AbstractService<MemberNotificationSettings, MemberNotificationSettingsRepository>() {
    @Inject private lateinit var memberNotificationSettingsRepository: MemberNotificationSettingsRepository
    @Inject private lateinit var memberCacheService: MemberCacheService
    @Inject private lateinit var memberNotificationSettingsFactory: MemberNotificationSettingsFactory
    @Inject private lateinit var traceActionService: TraceActionService

    override fun getRepository() = memberNotificationSettingsRepository

    fun findByMember(member: Member) = memberNotificationSettingsRepository.findByMember(member)

    fun update(memberUuid: UUID, dto: MemberNotificationSettingsDto): Response {
        val member = memberCacheService.find(memberUuid) ?: return Response.notFound()

        return if (findByMember(member) == null) {
            val settings = MemberNotificationSettings(
                member = member,
                notificationType = dto.type,
                platforms = dto.platforms.mapNotNull { Platform.fromNullable(it.id) }.sorted().toMutableSet()
            )

            val saved = memberNotificationSettingsRepository.save(settings)
            traceActionService.createTraceAction(saved, TraceAction.Action.CREATE)
            MapCache.invalidate(MemberNotificationSettings::class.java)
            Response.created(memberNotificationSettingsFactory.toDto(saved))
        } else {
            val settings = findByMember(member)!!
            var hasChanged = false

            if (settings.notificationType != dto.type) {
                settings.notificationType = dto.type
                hasChanged = true
            }

            val platforms = dto.platforms.mapNotNull { Platform.fromNullable(it.id) }.sorted().toMutableSet()

            if (!settings.platforms.sorted().toTypedArray().contentEquals(platforms.toTypedArray())) {
                settings.platforms = platforms
                hasChanged = true
            }

            if (!hasChanged) {
                return Response.notModified()
            }

            settings.lastUpdateDateTime = ZonedDateTime.now()
            val updated = memberNotificationSettingsRepository.update(settings)
            traceActionService.createTraceAction(updated, TraceAction.Action.UPDATE)
            MapCache.invalidate(MemberNotificationSettings::class.java)
            Response.ok(memberNotificationSettingsFactory.toDto(updated))
        }
    }
}