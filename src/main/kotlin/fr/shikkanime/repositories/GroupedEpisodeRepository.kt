package fr.shikkanime.repositories

import fr.shikkanime.entities.Anime_
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.miscellaneous.Pageable
import fr.shikkanime.entities.views.GroupedEpisodeView
import fr.shikkanime.entities.views.GroupedEpisodeView_

class GroupedEpisodeRepository : AbstractRepository<GroupedEpisodeView>() {
    override fun getEntityClass() = GroupedEpisodeView::class.java

    fun findAllBy(
        countryCode: CountryCode,
        page: Int,
        limit: Int,
    ): Pageable<GroupedEpisodeView> {
        return database.entityManager.use {
            val cb = it.criteriaBuilder
            val query = cb.createQuery(getEntityClass())
            val root = query.from(getEntityClass())

            query.where(
                cb.equal(root[GroupedEpisodeView_.anime][Anime_.countryCode], countryCode),
            ).orderBy(
                cb.desc(root[GroupedEpisodeView_.minReleaseDateTime]),
                cb.desc(root[GroupedEpisodeView_.anime][Anime_.name]),
                cb.desc(root[GroupedEpisodeView_.minSeason]),
                cb.desc(root[GroupedEpisodeView_.episodeType]),
                cb.desc(root[GroupedEpisodeView_.minNumber]),
            )

            buildPageableQuery(createReadOnlyQuery(it, query), page, limit)
        }
    }
}