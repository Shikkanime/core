package fr.shikkanime.database.repositories

import fr.shikkanime.database.entities.UserEntity
import fr.shikkanime.database.services.UserService
import fr.shikkanime.models.User
import fr.shikkanime.models.UserRole
import org.jetbrains.exposed.v1.crypt.Hashed
import kotlin.uuid.Uuid

/**
 * Persistence port of the user accounts, read as [User] domain models.
 *
 * Internal to the module: the [UserEntity] rows and the [Hashed] credentials they carry never
 * cross the `:database` boundary — applications go through [UserService].
 *
 * @see [UserService]
 */
internal abstract class UserRepository :
    ModelRepository<Uuid, UserEntity, User>(UserEntity) {
    /**
     * Loads every user carrying at least one of the given roles.
     *
     * @param roles roles to match.
     * @return the matching users.
     * @see [User]
     */
    abstract fun findAllByRoles(vararg roles: UserRole): List<User>

    /**
     * Finds a user by its external identifier.
     *
     * @param identifier external identifier to search for.
     * @return the matching user, or `null` when it is not known yet.
     * @see [User]
     */
    abstract fun findByIdentifier(identifier: ByteArray): User?

    /**
     * Finds a user by its username.
     *
     * @param username username to search for.
     * @return the matching user, or `null` when it is not known yet.
     * @see [User]
     */
    abstract fun findByUsername(username: String): User?

    /**
     * Persists a new user account with its already hashed password.
     *
     * The plain password is hashed by the caller before it reaches this port, so the repository
     * never sees a credential in clear.
     *
     * @param identifier external identifier of the user, when known.
     * @param username username of the user, when known.
     * @param hashedPassword hashed password of the user, when known.
     * @param roles roles granted to the user, when known.
     * @return the user as stored in database.
     * @see [User]
     */
    abstract fun save(
        identifier: ByteArray?,
        username: String?,
        hashedPassword: Hashed?,
        roles: Set<UserRole>?
    ): User
}
