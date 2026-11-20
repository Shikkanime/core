package fr.shikkanime.admin.controllers

import fr.shikkanime.admin.dtos.requests.UserLoginDtoRequest
import fr.shikkanime.admin.dtos.user.AdminUserDto
import fr.shikkanime.admin.usecases.LoginAdminUserUseCase
import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.ktor.*
import fr.shikkanime.ktor.dtos.ErrorMessageDto
import fr.shikkanime.ktor.dtos.MessageDto
import org.koin.core.annotation.Single
import java.util.logging.Level

@Suppress("unused") // Bound by Koin and routed from its annotations
@Single(binds = [IController::class])
@RestController("/v1/users")
class UserController(
    private val loginAdminUseCase: LoginAdminUserUseCase
) : IController {
    private val logger = LoggerFactory.getLogger()

    /**
     * Logs an admin user in.
     *
     * @param request login request carrying the username and password of the admin user.
     * @return the authenticated administrator, or an error message when the credentials are
     * not valid.
     * @see [LoginAdminUserUseCase]
     * @see [UserLoginDtoRequest]
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login an admin user",
        description = "Login an admin user with the provided username and password",
        tags = ["User"]
    )
    @ApiResponses([
        ApiResponse(200, "Successfully logged in", AdminUserDto::class),
        ApiResponse(400, "Invalid username or password", ErrorMessageDto::class),
    ])
    fun login(
        @RequestBody @Valid request: UserLoginDtoRequest
    ): ResponseEntity<*> {
        return try {
            // Authenticate the credentials and answer with the administrator they belong to
            ResponseEntity.ok(
                loginAdminUseCase.login(
                    username = request.username,
                    password = request.password
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.log(Level.SEVERE, "Login failed for user ${request.username}: ${e.message}", e)
            ResponseEntity.badRequest(MessageDto.error("Invalid username or password"))
        }
    }
}