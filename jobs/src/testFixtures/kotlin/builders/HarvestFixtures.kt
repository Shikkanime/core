package fr.shikkanime.jobs.builders

import fr.shikkanime.jobs.SmartResponse
import fr.shikkanime.jobs.builders.impl.PlatformAnimeLocalizationMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformAnimeMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformEpisodeLocalizationMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformEpisodeMockKBuilder
import fr.shikkanime.jobs.builders.impl.PlatformImageMockKBuilder
import fr.shikkanime.jobs.platforms.PlatformAnime
import fr.shikkanime.jobs.platforms.PlatformEpisode
import fr.shikkanime.jobs.platforms.PlatformEpisodeLocalization
import fr.shikkanime.models.Country
import fr.shikkanime.models.EpisodeType
import fr.shikkanime.models.ImageType
import fr.shikkanime.models.Locale
import fr.shikkanime.models.Platform
import fr.shikkanime.models.canonicalTitle
import java.time.ZoneOffset
import java.time.ZonedDateTime

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
 * @param image artwork URL of the episode.
 * @param releaseDateTime release date published by the platform.
 * @param season season of the episode.
 * @param number number of the episode in its season.
 * @param episodeType editorial category of the episode.
 * @param duration duration of the episode, in seconds.
 * @param url watch URL of the episode.
 * @param audioLocales audio tracks the platform publishes.
 * @param uncensored whether the release is uncensored.
 * @param localizations localized texts published by the platform.
 * @return the platform episode fixture.
 * @see [PlatformEpisodeMockKBuilder]
 */
fun harvestedEpisode(
    anime: PlatformAnime,
    id: String = "1001",
    image: String = "https://example.test/episode.png",
    releaseDateTime: ZonedDateTime = ZonedDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC),
    season: Int = 1,
    number: Int = 1,
    episodeType: EpisodeType = EpisodeType.EPISODE,
    duration: Long = 1440,
    url: String = "https://example.test/episodes/$id",
    audioLocales: List<Locale> = listOf(Country.FR.defaultLocale),
    uncensored: Boolean = false,
    localizations: List<PlatformEpisodeLocalization> = listOf(
        PlatformEpisodeLocalizationMockKBuilder {
            country = Country.FR
            title = "Episode"
            description = "Episode description"
        }.build()
    )
): PlatformEpisode =
    PlatformEpisodeMockKBuilder {
        this.anime = anime
        this.id = id
        this.image = image
        this.releaseDateTime = releaseDateTime
        this.season = season
        this.number = number
        this.episodeType = episodeType
        this.duration = duration
        this.url = url
        this.audioLocales = audioLocales
        this.uncensored = uncensored
        this.localizations = localizations
    }.build()

/**
 * Builds one harvested anime exactly as a streaming platform produces it.
 *
 * @param id identifier of the anime on the streaming platform.
 * @param title localized title of the harvested anime.
 * @param description localized description of the harvested anime.
 * @param platform streaming platform the anime is harvested from.
 * @param image artwork URL of the harvested anime.
 * @return the platform anime fixture.
 * @see [PlatformAnimeMockKBuilder]
 */
fun harvestedAnime(
    id: String = "123",
    title: String = "Title",
    description: String = "Description",
    platform: Platform = Platform.ANIMATION_DIGITAL_NETWORK,
    image: String = "https://example.test/banner.png"
): PlatformAnime =
    PlatformAnimeMockKBuilder {
        this.id = id
        this.platform = platform
        normalizedTitle = canonicalTitle(mapOf(Country.FR.defaultLocale to title))
        images = listOf(
            PlatformImageMockKBuilder {
                type = ImageType.BANNER
                url = image
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
