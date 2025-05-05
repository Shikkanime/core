package fr.shikkanime.dtos

import fr.shikkanime.entities.MemberNotificationSettings

data class MemberNotificationSettingsDto(
    val type: MemberNotificationSettings.NotificationType,
    val platforms: Set<PlatformDto>,
)
