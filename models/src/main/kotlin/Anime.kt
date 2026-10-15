package fr.shikkanime.models

import kotlinx.datetime.LocalDateTime
import kotlin.uuid.Uuid

/**
 * Canonical anime identity, shared by every country and language.
 *
 * Translatable text lives in [AnimeLocalization] and artwork in [Image], so this model only
 * aggregates the rows owned by the anime without carrying any localized content itself.
 *
 * @property images Artwork rows owned by this anime.
 * @property localizations Localized titles and descriptions of this anime.
 * @property platforms Platform source links of this anime.
 */
data class Anime(
    override val id: Uuid,
    override val createdAt: LocalDateTime,
    override val updatedAt: LocalDateTime,
    val images: List<Image>,
    val localizations: List<AnimeLocalization>,
    val platforms: List<AnimePlatform>
) : ShikkModel(id, createdAt, updatedAt)
