package fr.shikkanime.factories.impl

import com.google.inject.Inject
import fr.shikkanime.dtos.MemberNotificationSettingsDto
import fr.shikkanime.entities.MemberNotificationSettings
import fr.shikkanime.factories.IGenericFactory

class MemberNotificationSettingsFactory : IGenericFactory<MemberNotificationSettings, MemberNotificationSettingsDto> {
    @Inject private lateinit var platformFactory: PlatformFactory

    override fun toDto(entity: MemberNotificationSettings): MemberNotificationSettingsDto {
        return MemberNotificationSettingsDto(
            type = entity.notificationType,
            platforms = entity.platforms.map { platformFactory.toDto(it) }.toSet(),
        )
    }
}