package fr.shikkanime.jobs.platforms

import fr.shikkanime.core.TimeUtils
import fr.shikkanime.models.Anime
import fr.shikkanime.models.AnimePlatform
import fr.shikkanime.models.Platform
import kotlin.uuid.Uuid

/**
 * Normalized representation of an anime title harvested from a streaming platform.
 *
 * This entity never carries translatable text nor fixed artwork fields: titles and synopses live
 * in [PlatformAnimeLocalization] and artwork in [PlatformImage], so new languages and image kinds
 * are added without reshaping it.
 *
 * @property id Identifier of the show assigned by the source platform.
 * @property images Artwork made available by the platform for this show.
 * @property localizations Localized editorial texts of the show, one per eligible country.
 */
data class PlatformAnime(
    val id: String,
    val images: List<PlatformImage>,
    val localizations: List<PlatformAnimeLocalization>
) {
    /**
     * Maps this harvested show to the canonical anime aggregate to persist.
     *
     * The platform source link is rebuilt on every call, and the children are mapped through their
     * own [PlatformImage.toModel] and [PlatformAnimeLocalization.toModel].
     *
     * @param platform streaming platform this show was harvested from.
     * @param id identifier to give to the anime: the stored one behind the platform link on an
     * update, a fresh one on a first harvest.
     * @return the [Anime] to pass to the persistence.
     * @see Anime
     */
    fun toModel(platform: Platform, id: Uuid): Anime =
        Anime(
            id = id,
            createdAt = TimeUtils.now(),
            updatedAt = TimeUtils.now(),
            images = images.map { it.toModel(id) },
            localizations = localizations.map(PlatformAnimeLocalization::toModel),
            platforms = listOf(
                AnimePlatform(
                    id = Uuid.random(),
                    createdAt = TimeUtils.now(),
                    updatedAt = TimeUtils.now(),
                    platform = platform,
                    platformId = this.id
                )
            )
        )
}
