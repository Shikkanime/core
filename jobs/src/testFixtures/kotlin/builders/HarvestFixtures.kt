package fr.shikkanime.jobs.builders

import fr.shikkanime.jobs.SmartResponse
import fr.shikkanime.jobs.builders.impl.PlatformAnimeLocalizationMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformAnimeMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformEpisodeMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformImageMockKBuilder
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.models.Country
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Platform
import fr.shikkanime.models.canonicalTitle

/**
 * Wraps the given episodes as the harvest of one streaming platform.
 *
 * @param changed whether the platform reports a change in its payload.
 * @param episodes episodes returned by the platform.
 * @return the platform harvest to pass to the ingestion.
 * @see [SmartResponse]
 */
fun harvest(
    changed: Boolean,
    vararg episodes: PlatformEpisode
): SmartResponse<List<PlatformEpisode>> =
    SmartResponse(episodes.toList(), changed)

/**
 * Builds one harvested episode carrying the given anime.
 *
 * @param anime harvested anime carried by the episode.
 * @param id identifier of the episode on the streaming platform.
 * @return the platform episode fixture.
 * @see [PlatformEpisodeMockKBuilder]
 */
fun harvestedEpisode(anime: PlatformAnime, id: String = "1001"): PlatformEpisode =
    PlatformEpisodeMockKBuilder {
        this.anime = anime
        this.id = id
    }.build()

/**
 * Builds one harvested anime exactly as a streaming platform produces it.
 *
 * @param id identifier of the anime on the streaming platform.
 * @param title localized title of the harvested anime.
 * @param description localized description of the harvested anime.
 * @param platform streaming platform the anime is harvested from.
 * @return the platform anime fixture.
 * @see [PlatformAnimeMockKBuilder]
 */
fun harvestedAnime(
    id: String = "123",
    title: String = "Title",
    description: String = "Description",
    platform: Platform = Platform.ANIMATION_DIGITAL_NETWORK
): PlatformAnime =
    PlatformAnimeMockKBuilder {
        this.id = id
        this.platform = platform
        normalizedTitle = canonicalTitle(mapOf(Country.FR.defaultLocale to title))
        images = listOf(
            PlatformImageMockKBuilder {
                type = ImageType.BANNER
                url = "https://example.test/banner.png"
            }.build()
        )
        localizations = listOf(
            PlatformAnimeLocalizationMockKBuilder {
                country = Country.FR
                locale = Country.FR.defaultLocale
                this.title = title
                this.description = description
            }.build()
        )
    }.build()