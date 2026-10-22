package fr.shikkanime.database

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VarCharColumnType
import kotlin.reflect.KClass

class SetEnumColumnTransformer<T : Enum<T>>(
    private val enumClass: KClass<T>
) : ColumnTransformer<String, Set<T>> {
    private val enumConstants by lazy {
        enumClass.java.enumConstants?.associateBy(Enum<T>::name) ?: emptyMap()
    }

    override fun unwrap(value: Set<T>): String =
        value.joinToString(separator = ",") { it.name }

    private fun toEnumConstant(name: String): T =
        enumConstants[name]
            ?: error("'$name' can't be associated with any value from ${enumClass.qualifiedName}")

    override fun wrap(value: String): Set<T> =
        if (value.isBlank())
            emptySet()
        else
            value.split(",").map(::toEnumConstant).toSet()
}

inline fun <reified T : Enum<T>> Table.enumSet(name: String): Column<Set<T>> =
    registerColumn(name, VarCharColumnType())
        .transform(SetEnumColumnTransformer(T::class))
