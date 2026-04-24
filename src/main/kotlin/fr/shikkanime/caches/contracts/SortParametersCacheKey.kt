package fr.shikkanime.caches.contracts

import fr.shikkanime.entities.miscellaneous.SortParameter

interface SortParametersCacheKey {
    val sort: List<SortParameter>
}
