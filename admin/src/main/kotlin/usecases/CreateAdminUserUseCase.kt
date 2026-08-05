package fr.shikkanime.admin.usecases

import fr.shikkanime.exposed.Transactional

/**
 * Use case responsible for initializing the default administrator account.
 */
interface CreateAdminUserUseCase {
    /**
     * Creates an initial administrator user with a randomly generated password if no admin account exists yet.
     */
    @Transactional
    fun execute()
}
