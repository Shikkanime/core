package fr.shikkanime.caches.contracts

import fr.shikkanime.entities.enums.LangType

interface SearchTypesCacheKey {
    val searchTypes: Array<LangType>?
}
