package fr.shikkanime.database.services

import fr.shikkanime.database.entities.UserEntity
import fr.shikkanime.exposed.Transactional
import fr.shikkanime.models.UserRole

interface UserService {
    @Transactional
    fun create(
        identifier: ByteArray? = null,
        username: String? = null,
        password: String? = null,
        roles: Set<UserRole>? = null
    ): UserEntity
}