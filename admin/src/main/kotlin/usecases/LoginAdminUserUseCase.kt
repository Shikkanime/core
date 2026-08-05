package fr.shikkanime.admin.usecases

import fr.shikkanime.exposed.Transactional

interface LoginAdminUserUseCase {
    @Transactional
    @Throws(IllegalArgumentException::class)
    fun login(username: String, password: String)
}