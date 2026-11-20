package fr.shikkanime.admin.usecases

import fr.shikkanime.admin.dtos.user.AdminUserDto
import fr.shikkanime.exposed.Transactional

/**
 * Authenticates an administrator against the stored user accounts.
 *
 * The business rules of a sign-in - the account must exist, carry the admin role and a matching
 * credential - are documented here so the implementation stays a thin orchestration.
 */
interface LoginAdminUserUseCase {
    /**
     * Logs an administrator in with the submitted credentials.
     *
     * @param username username of the account to authenticate.
     * @param password password submitted in clear, checked against the stored credential.
     * @return the authenticated administrator.
     * @throws IllegalArgumentException when the account does not exist, is not an admin or the
     * password does not match.
     * @see [AdminUserDto]
     */
    @Transactional
    @Throws(IllegalArgumentException::class)
    fun login(username: String, password: String): AdminUserDto
}
