package fr.shikkanime.database.services

import fr.shikkanime.exposed.Transactional
import fr.shikkanime.models.User
import fr.shikkanime.models.UserRole

/**
 * Business port of the user accounts, the only way applications reach them.
 *
 * Reads return the [User] domain model; the password hash never leaves the module, and
 * [passwordMatches] is the single verification entry point.
 *
 * @see [User]
 */
interface UserService {
    /**
     * Finds a user by its username.
     *
     * @param username username to search for.
     * @return the matching user, or `null` when it is not known yet.
     * @see [User]
     */
    @Transactional
    fun findByUsername(username: String): User?

    /**
     * Loads every user carrying the given roles.
     *
     * @param roles roles to match.
     * @return the matching users.
     * @see [User]
     */
    @Transactional
    fun findAllByRoles(vararg roles: UserRole): List<User>

    /**
     * Tells whether a plain password matches the stored credential of a user.
     *
     * The comparison runs against the one-way hash owned by the persistence layer, so neither
     * the hash nor the plain password leaves the module.
     *
     * @param user user to check the password of.
     * @param plainPassword password submitted in clear.
     * @return `true` when the password matches the stored credential.
     * @see [User]
     */
    @Transactional
    fun passwordMatches(user: User, plainPassword: String): Boolean

    /**
     * Creates a user account, hashing its password before persistence.
     *
     * At least an identifier or a username is required, and both must be unused: an account
     * already known under either refuses the creation.
     *
     * @param identifier external identifier of the user, when known.
     * @param username username of the user, when known.
     * @param password plain password of the user, hashed before it is stored, when known.
     * @param roles roles granted to the user, when known.
     * @return the created user.
     * @throws IllegalArgumentException when neither identifier nor username is provided, or
     * when one of them is already used.
     * @see [User]
     */
    @Transactional
    fun create(
        identifier: ByteArray? = null,
        username: String? = null,
        password: String? = null,
        roles: Set<UserRole>? = null
    ): User
}
