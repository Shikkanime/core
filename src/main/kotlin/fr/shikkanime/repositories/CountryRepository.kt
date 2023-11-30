package fr.shikkanime.repositories

import fr.shikkanime.entities.Country

class CountryRepository : AbstractRepository<Country>() {
    fun findByName(name: String): Country? {
        return getEntityManager().createQuery("FROM Country WHERE name = :name", getEntityClass())
            .setParameter("name", name)
            .resultList
            .firstOrNull()
    }

    fun findByCode(code: String): Country? {
        return getEntityManager().createQuery("FROM Country WHERE countryCode = :code", getEntityClass())
            .setParameter("code", code)
            .resultList
            .firstOrNull()
    }

    fun findAllByCode(codes: List<String>): List<Country> {
        return getEntityManager().createQuery("FROM Country WHERE countryCode IN :codes", getEntityClass())
            .setParameter("codes", codes)
            .resultList
    }
}