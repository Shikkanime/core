package fr.shikkanime.admin.controllers

import fr.shikkanime.admin.dtos.requests.UserLoginDtoRequest
import fr.shikkanime.admin.usecases.LoginAdminUserUseCase
import fr.shikkanime.core.LoggerFactory
import fr.shikkanime.ktor.ApiResponse
import fr.shikkanime.ktor.ApiResponses
import fr.shikkanime.ktor.IController
import fr.shikkanime.ktor.Operation
import fr.shikkanime.ktor.PostMapping
import fr.shikkanime.ktor.RequestBody
import fr.shikkanime.ktor.ResponseEntity
import fr.shikkanime.ktor.RestController
import fr.shikkanime.ktor.Valid
import fr.shikkanime.ktor.dtos.ErrorMessageDto
import fr.shikkanime.ktor.dtos.MessageDto
import org.koin.core.annotation.Single
import java.util.logging.Level

@Single(binds = [IController::class])
@RestController("/api/users")
class UserController(
    private val loginAdminUseCase: LoginAdminUserUseCase
) : IController {
    private val logger = LoggerFactory.getLogger()

    @PostMapping("/login")
    @Operation(
        summary = "Login an admin user",
        description = "Login an admin user with the provided username and password",
        tags = ["User"]
    )
    @ApiResponses([
        ApiResponse(200, "Successfully logged in", String::class),
        ApiResponse(400, "Invalid username or password", ErrorMessageDto::class),
    ])
    fun login(
        @RequestBody @Valid request: UserLoginDtoRequest
    ): ResponseEntity<*> {
        return try {
            loginAdminUseCase.login(
                username = request.username,
                password = request.password
            )

            ResponseEntity.ok("OK")
        } catch (e: IllegalArgumentException) {
            logger.log(Level.SEVERE, "Login failed for user ${request.username}: ${e.message}", e)
            ResponseEntity.badRequest(MessageDto.error("Invalid username or password"))
        }
    }
}