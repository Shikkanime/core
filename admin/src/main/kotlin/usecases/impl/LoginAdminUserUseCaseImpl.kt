package fr.shikkanime.admin.usecases.impl

import fr.shikkanime.admin.dtos.user.AdminUserDto
import fr.shikkanime.admin.dtos.user.toAdminDto
import fr.shikkanime.admin.usecases.LoginAdminUserUseCase
import fr.shikkanime.database.services.UserService
import fr.shikkanime.models.UserRole
import org.koin.core.annotation.Single

@Suppress("unused")
@Single
class LoginAdminUserUseCaseImpl(
    private val userService: UserService,
) : LoginAdminUserUseCase {

    override fun login(username: String, password: String): AdminUserDto {
        // Resolve the account behind the submitted username before checking its rights
        val user = requireNotNull(userService.findByUsername(username)) {
            "User with username $username not found"
        }
        require(user.roles?.contains(UserRole.ADMIN) == true) { "User with username $username is not an admin" }
        require(userService.passwordMatches(user, password)) { "Invalid password for user $username" }
        // TODO: Generate a JWT token
        return user.toAdminDto()
    }
}
