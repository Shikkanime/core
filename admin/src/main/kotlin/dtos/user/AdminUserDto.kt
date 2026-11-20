package fr.shikkanime.admin.dtos.user

import fr.shikkanime.models.User
import fr.shikkanime.models.UserRole
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Administration view of one user account.
 *
 * Carries the identity of the account and its granted roles; the credential never leaves the
 * persistence module, so no password field exists here.
 */
@Serializable
data class AdminUserDto(
    val id: Uuid,
    val username: String?,
    val roles: Set<UserRole>?
)

/**
 * Maps a stored user to its administration view.
 *
 * @receiver stored user account.
 * @return the administration view of this user.
 * @see [AdminUserDto]
 * @see [User]
 */
fun User.toAdminDto(): AdminUserDto =
    AdminUserDto(
        id = requireNotNull(id),
        username = username,
        roles = roles
    )
