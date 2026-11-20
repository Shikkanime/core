package fr.shikkanime.admin.usecases.impl

import fr.shikkanime.admin.usecases.CreateAdminUserUseCase
import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.core.StringUtils
import fr.shikkanime.database.services.UserService
import fr.shikkanime.models.UserRole
import org.koin.core.annotation.Single
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

@Suppress("unused")
@Single
class CreateAdminUserUseCaseImpl(
    private val userService: UserService,
) : CreateAdminUserUseCase {
    private val logger = LoggerFactory.getLogger()

    override fun execute() {
        // Check if an administrator already exists before creating a new one
        val users = userService.findAllByRoles(UserRole.ADMIN)

        if (users.isNotEmpty()) {
            logger.info("Admin user already exists")
            return
        }

        // Generate the initial credential without ever writing it to the logs
        val defaultPassword = StringUtils.generateRandomString(32, random = SecureRandom().asKotlinRandom())

        // Create the initial admin account with a secure generated password
        userService.create(
            username = "admin",
            password = defaultPassword,
            roles = setOf(UserRole.ADMIN)
        )
    }
}
