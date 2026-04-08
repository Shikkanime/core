package fr.shikkanime.repositories

import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.miscellaneous.GenreCoverage
import fr.shikkanime.entities.miscellaneous.MarketShare

class AnalyticsRepository : AbstractRepository<EpisodeMapping>() {
    override fun getEntityClass() = EpisodeMapping::class.java

    fun getAllMarketShare(startYear: Int, endYear: Int): List<MarketShare> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(MarketShare::class.java)
            val root = query.from(getEntityClass())
            val variantJoin = root.join(EpisodeMapping_.variants)

            query.select(
                cb.construct(
                    MarketShare::class.java,
                    root[EpisodeMapping_.simulcast],
                    variantJoin[EpisodeVariant_.platform],
                    cb.countDistinct(root[EpisodeMapping_.anime][Anime_.uuid]).cast(Double::class.java),
                )
            ).where(cb.between(root[EpisodeMapping_.simulcast][Simulcast_.year], startYear, endYear))
                .groupBy(root[EpisodeMapping_.simulcast], variantJoin[EpisodeVariant_.platform])

            it.createQuery(query)
                .resultList
        }
    }

    fun getSubCoverage(countryCode: CountryCode, startYear: Int, endYear: Int): List<MarketShare> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(MarketShare::class.java)
            val root = query.from(getEntityClass())
            val variantJoin = root.join(EpisodeMapping_.variants)
            val simulcastJoin = root.join(EpisodeMapping_.simulcast)

            val subQuery = query.subquery(Long::class.java)
            val subRoot = subQuery.from(getEntityClass())
            val subVariantJoin = subRoot.join(EpisodeMapping_.variants)
            val subSimulcastJoin = subRoot.join(EpisodeMapping_.simulcast)

            val correlatedSimulcast = subQuery.correlate(simulcastJoin)
            val correlatedVariant = subQuery.correlate(variantJoin)

            subQuery.select(cb.countDistinct(subRoot[EpisodeMapping_.anime][Anime_.uuid]))
                .where(
                    cb.and(
                        cb.equal(subSimulcastJoin[Simulcast_.uuid], correlatedSimulcast[Simulcast_.uuid]),
                        cb.equal(subVariantJoin[EpisodeVariant_.platform], correlatedVariant[EpisodeVariant_.platform]),
                        cb.equal(subVariantJoin[EpisodeVariant_.audioLocale], countryCode.locale),
                    )
                )

            query.select(
                cb.construct(
                    MarketShare::class.java,
                    simulcastJoin,
                    variantJoin[EpisodeVariant_.platform],
                    cb.prod(
                        cb.quot(
                            subQuery.cast(Double::class.java),
                            cb.countDistinct(root[EpisodeMapping_.anime][Anime_.uuid]).cast(Double::class.java)
                        ),
                        100.0
                    ),
                )
            ).where(cb.between(simulcastJoin[Simulcast_.year], startYear, endYear))
                .groupBy(simulcastJoin, variantJoin[EpisodeVariant_.platform])

            it.createQuery(query)
                .resultList
        }
    }

    fun getAllGenreCoverage(startYear: Int, endYear: Int): List<GenreCoverage> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(GenreCoverage::class.java)
            val root = query.from(getEntityClass())

            val animeJoin = root.join(EpisodeMapping_.anime)
            val genreJoin = animeJoin.join(Anime_.genres)

            query.select(
                cb.construct(
                    GenreCoverage::class.java,
                    root[EpisodeMapping_.simulcast],
                    genreJoin,
                    cb.countDistinct(root[EpisodeMapping_.anime][Anime_.uuid]).cast(Double::class.java),
                )
            ).where(cb.between(root[EpisodeMapping_.simulcast][Simulcast_.year], startYear, endYear))
                .groupBy(root[EpisodeMapping_.simulcast], genreJoin)

            it.createQuery(query)
                .resultList
        }
    }
}