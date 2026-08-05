package fr.shikkanime.admin.usecases.impl

import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.database.repositories.UserRepository
import fr.shikkanime.database.services.UserService
import fr.shikkanime.admin.usecases.CreateAdminUserUseCase
import fr.shikkanime.core.StringUtils
import fr.shikkanime.models.UserRole
import org.koin.core.annotation.Single
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

@Suppress("unused")
@Single
class CreateAdminUserUseCaseImpl(
    private val userRepository: UserRepository,
    private val userService: UserService,
) : CreateAdminUserUseCase {
    private val logger = LoggerFactory.getLogger()

    override fun execute() {
        val users = userRepository.findAllByRoles(UserRole.ADMIN)

        if (users.isNotEmpty()) {
            logger.info("Admin user already exists")
            return
        }

        val defaultPassword = StringUtils.generateRandomString(32, random = SecureRandom().asKotlinRandom())
        logger.info("Generated default password: $defaultPassword")

        userService.create(
            username = "admin",
            password = defaultPassword,
            roles = setOf(UserRole.ADMIN)
        )
    }
}