package fr.shikkanime.admin.usecases

import fr.shikkanime.exposed.Transactional

interface CreateAdminUserUseCase {
    @Transactional
    fun execute()
}