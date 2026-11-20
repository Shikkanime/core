package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Canonical user account of the administration, shared by every sign-in method.
 *
 * The credential is deliberately not part of the model: the stored password is a one-way hash
 * owned by the persistence layer, and verification goes through the user service of the
 * persistence module so the hash never crosses the module boundary.
 *
 * @see [UserRole]
 */
data class User(
    override val id: Uuid?,
    override val createdAt: LocalDateTime?,
    override val updatedAt: LocalDateTime?,
    val identifier: List<Byte>?,
    val username: String?,
    val roles: Set<UserRole>?
) : ShikkModel(id, createdAt, updatedAt)
