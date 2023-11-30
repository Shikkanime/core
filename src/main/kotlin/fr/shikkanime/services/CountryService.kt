package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.Country
import fr.shikkanime.repositories.CountryRepository
import fr.shikkanime.utils.MapCache

class CountryService : AbstractService<Country, CountryRepository>() {
    @Inject
    private lateinit var countryRepository: CountryRepository

    override fun getRepository(): CountryRepository {
        return countryRepository
    }

    fun findByName(name: String): Country? {
        return countryRepository.findByName(name)
    }

    fun findByCode(code: String): Country? {
        return countryRepository.findByCode(code)
    }

    override fun save(entity: Country): Country {
        MapCache.invalidate(Country::class.java)
        return super.save(entity)
    }
}