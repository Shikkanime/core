package fr.shikkanime.entities.miscellaneous

data class SortParameter(
    val field: String,
    val order: Order,
) {
    enum class Order {
        ASC,
        DESC,
        ;
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SortParameter

        if (field != other.field) return false
        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = field.hashCode()
        result = 31 * result + order.hashCode()
        return result
    }

    override fun toString() = "$field,$order"
}