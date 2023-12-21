package fr.shikkanime.entities

data class SortParameter(
    val field: String,
    val order: Order,
) {
    enum class Order {
        ASC,
        DESC
    }
}
