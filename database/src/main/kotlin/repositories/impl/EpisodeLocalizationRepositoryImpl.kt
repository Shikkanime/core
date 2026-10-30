package fr.shikkanime.database.repositories.impl

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.database.entities.EpisodeLocalizationEntity
import fr.shikkanime.database.entities.EpisodeLocalizationTable
import fr.shikkanime.database.entities.EpisodeTable
import fr.shikkanime.database.repositories.EpisodeLocalizationRepository
import fr.shikkanime.models.EpisodeLocalization
import fr.shikkanime.models.Locale
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Suppress("unused")
@Single(binds = [EpisodeLocalizationRepository::class])
internal class EpisodeLocalizationRepositoryImpl : EpisodeLocalizationRepository() {

    override fun findAllByEpisodeIds(episodeIds: List<Uuid>): Map<Uuid?, List<EpisodeLocalization>> =
        EpisodeLocalizationEntity.find { EpisodeLocalizationTable.episode inList episodeIds }
            .map(EpisodeLocalizationEntity::toModel)
            .groupBy(EpisodeLocalization::episodeId)

    /**
     * Finds one localization row of an episode by its locale.
     *
     * @param episodeId identifier of the episode owning the localization.
     * @param locale locale of the localized text to load.
     * @return the matching entity, or `null` when the locale is not translated yet.
     * @see [EpisodeLocalization]
     */
    private fun findByIdAndLocale(episodeId: Uuid, locale: Locale): EpisodeLocalizationEntity? =
        EpisodeLocalizationEntity.find {
            (EpisodeLocalizationTable.episode eq episodeId) and
                (EpisodeLocalizationTable.locale eq locale)
        }
            .limit(1)
            .firstOrNull()

    override fun save(localization: EpisodeLocalization): EpisodeLocalization =
        findByIdAndLocale(requireNotNull(localization.episodeId), localization.locale)
            .ifExists {
                if (title != localization.title || description != localization.description) {
                    title = localization.title
                    description = localization.description
                    updatedAt = TimeUtils.now()
                }
            }
            .newIfNotExists(localization.id) {
                // The row belongs to the episode its localization already references
                episode = EntityID(requireNotNull(localization.episodeId), EpisodeTable)
                locale = localization.locale
                title = localization.title
                description = localization.description
            }
            .applyFlush()
            .toModel()
}
