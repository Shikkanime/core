package fr.shikkanime.admin.usecases

import fr.shikkanime.database.services.UserService
import fr.shikkanime.exposed.Transactional

/**
 * Use case responsible for initializing the default administrator account.
 */
interface CreateAdminUserUseCase {
    /**
     * Creates an initial administrator user with a randomly generated password if no admin account exists yet.
     *
     * @return nothing; the account is created as a side effect, or left untouched when an
     * administrator already exists.
     * @see [UserService]
     */
    @Transactional
    fun execute()
}
