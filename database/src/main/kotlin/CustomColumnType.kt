package fr.shikkanime.database

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType
import kotlin.reflect.KClass

/**
 * Column transformer storing a set of enum constants as a comma-separated string.
 *
 * The persisted form is the constant names joined by commas, so the column stays a plain
 * `varchar` while the entity exposes a [Set] of the enum type.
 *
 * @param enumClass enum type the column carries.
 * @see [enumSet]
 */
internal class SetEnumColumnTransformer<T : Enum<T>>(
    private val enumClass: KClass<T>
) : ColumnTransformer<String, Set<T>> {
    private val enumConstants by lazy {
        enumClass.java.enumConstants?.associateBy(Enum<T>::name) ?: emptyMap()
    }

    /**
     * Resolves one stored constant name back to its enum value.
     *
     * @param name constant name read from the column.
     * @return the matching enum constant.
     * @throws IllegalStateException when the name belongs to no constant of the enum.
     */
    private fun toEnumConstant(name: String): T =
        enumConstants[name]
            ?: error("'$name' can't be associated with any value from ${enumClass.qualifiedName}")

    override fun unwrap(value: Set<T>): String =
        value.joinToString(separator = ",") { it.name }

    override fun wrap(value: String): Set<T> =
        if (value.isBlank())
            emptySet()
        else
            value.split(",").map(::toEnumConstant).toSet()
}

/**
 * Declares a varchar column holding a set of enum constants.
 *
 * @param T enum type the column carries.
 * @param name column name.
 * @return the column exposing a [Set] of the enum type.
 * @see [SetEnumColumnTransformer]
 */
internal inline fun <reified T : Enum<T>> Table.enumSet(name: String): Column<Set<T>> =
    registerColumn(name, VarCharColumnType())
        .transform(SetEnumColumnTransformer(T::class))
