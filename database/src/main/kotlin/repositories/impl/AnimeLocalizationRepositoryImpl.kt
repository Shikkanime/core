package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.entities.AnimeLocalizationEntity
import fr.shikkanime.database.entities.AnimeLocalizationTable
import fr.shikkanime.database.repositories.AnimeLocalizationRepository
import fr.shikkanime.database.repositories.AnimeRepository
import fr.shikkanime.models.AnimeLocalization
import fr.shikkanime.models.Locale
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [AnimeLocalizationRepository::class])
internal class AnimeLocalizationRepositoryImpl(
    private val animeRepository: AnimeRepository
) : AnimeLocalizationRepository() {

    override fun findAllByAnimeIds(animeIds: List<Uuid>): Map<Uuid?, List<AnimeLocalization>> =
        AnimeLocalizationEntity.find { AnimeLocalizationTable.anime inList animeIds }
            .map(AnimeLocalizationEntity::toModel)
            .groupBy(AnimeLocalization::animeId)

    /**
     * Finds one localization row of an anime by its locale.
     *
     * @param animeId identifier of the anime owning the localization.
     * @param locale locale of the localized text to load.
     * @return the matching entity, or `null` when the locale is not translated yet.
     * @see [AnimeLocalization]
     */
    private fun findByIdAndLocale(animeId: Uuid, locale: Locale): AnimeLocalizationEntity? =
        AnimeLocalizationEntity.find {
            (AnimeLocalizationTable.anime eq animeId) and
                (AnimeLocalizationTable.locale eq locale)
        }
            .limit(1)
            .firstOrNull()

    override fun save(localization: AnimeLocalization): AnimeLocalization =
        findByIdAndLocale(requireNotNull(localization.animeId), localization.locale)
            .ifExists {
                if (title != localization.title || description != localization.description) {
                    title = localization.title
                    description = localization.description
                    updatedAt = TimeUtils.now()
                }
            }
            .newIfNotExists(localization.id) {
                // The row belongs to the anime its localization already references
                this.anime = animeRepository.getReference(requireNotNull(localization.animeId))
                locale = localization.locale
                title = localization.title
                description = localization.description
            }
            .applyFlush()
            .toModel()
}
