package fr.shikkanime.admin.usecases.impl

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.repositories.UserRepository
import fr.shikkanime.database.services.UserService
import fr.shikkanime.admin.usecases.CreateAdminUserUseCase
import fr.shikkanime.admin.usecases.LoginAdminUserUseCase
import fr.shikkanime.core.StringUtils
import fr.shikkanime.models.UserRole
import org.koin.core.annotation.Single
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

@Suppress("unused")
@Single
class LoginAdminUserUseCaseImpl(
    private val userRepository: UserRepository,
) : LoginAdminUserUseCase {
    private val logger = LoggerFactory.getLogger()

    override fun login(username: String, password: String) {
        val user = requireNotNull(userRepository.findByUsername(username)) { "User with username $username not found" }
        val userPassword = requireNotNull(user.password) { "User with username $username has no password set" }
        require(user.roles?.contains(UserRole.ADMIN) == true) { "User with username $username is not an admin" }
        require(userPassword.matches(password)) { "Invalid password for user $username" }
        // TODO: Generate a JWT token
    }
}