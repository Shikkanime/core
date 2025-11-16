package fr.shikkanime.services

import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.StringUtils
import fr.shikkanime.utils.atStartOfWeek
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertTrue

class AnimeServiceTest : AbstractTest() {
    @Test
    fun getWeeklyAnimes_shouldGroupMultipleLangTypes_whenReleasedOnSameHour() {
        val now = ZonedDateTime.now().withHour(19).withMinute(30).withSecond(0).withNano(0)
        val previousWeek = now.minusWeeks(1)
        val anime = createAnime("A")

        val episodeMapping1 = createEpisodeMapping(anime, 1, 1, previousWeek)

        createEpisodeVariant(
            episodeMapping1,
            Platform.CRUN,
            "ja-JP",
            "test-1-CRUN-JA-JP",
            "test-1-CRUN.mp4",
            previousWeek
        )

        createEpisodeVariant(
            episodeMapping1,
            Platform.CRUN,
            "fr-FR",
            "test-1-CRUN-FR-FR",
            "test-1-CRUN.mp4",
            previousWeek.plusMinutes(10)
        )

        val episodeMapping2 = createEpisodeMapping(anime, 1, 2, now)

        createEpisodeVariant(
            episodeMapping2,
            Platform.CRUN,
            "ja-JP",
            "test-2-CRUN-JA-JP",
            "test-1-CRUN.mp4",
            now
        )

        episodeVariantService.preIndex()
        InvalidationService.invalidateAll()

        var weeklyReleases = animeService.getWeeklyAnimes(CountryCode.FR, null, now.toLocalDate().atStartOfWeek())
        var releases = weeklyReleases.flatMap { it.releases }
        assertEquals(1, releases.size)
        assertEquals(1, releases.first().langTypes.size)

        createEpisodeVariant(
            episodeMapping2,
            Platform.CRUN,
            "fr-FR",
            "test-2-CRUN-FR-FR",
            "test-1-CRUN.mp4",
            now.plusMinutes(10)
        )

        episodeVariantService.preIndex()
        InvalidationService.invalidateAll()

        weeklyReleases = animeService.getWeeklyAnimes(CountryCode.FR, null, now.toLocalDate().atStartOfWeek())
        releases = weeklyReleases.flatMap { it.releases }
        assertEquals(1, releases.size)
        assertEquals(2, releases.first().langTypes.size)
    }

    @Test
    fun getWeeklyAnimes_shouldCreateSeparateReleases_forDifferentEpisodesOnSameDay() {
        val now = ZonedDateTime.now().withHour(16).withMinute(0).withSecond(0).withNano(0)
        val previousWeek = now.minusWeeks(1)
        val anime = createAnime("A")

        val episodeMapping1 = createEpisodeMapping(anime, 1, 2, previousWeek)

        createEpisodeVariant(
            episodeMapping1,
            Platform.CRUN,
            "ja-JP",
            "test-2-CRUN-JA-JP",
            "test-1-CRUN.mp4",
            previousWeek
        )

        val episodeMapping2 = createEpisodeMapping(anime, 1, 1, previousWeek.plusHours(6))

        createEpisodeVariant(
            episodeMapping2,
            Platform.CRUN,
            "ja-JP",
            "test-1-CRUN-JA-JP",
            "test-1-CRUN.mp4",
            previousWeek.plusHours(6)
        )

        episodeVariantService.preIndex()
        InvalidationService.invalidateAll()

        var weeklyReleases = animeService.getWeeklyAnimes(CountryCode.FR, null, now.toLocalDate().atStartOfWeek())
        var releases = weeklyReleases.flatMap { it.releases }
        assertEquals(2, releases.size)
        assertTrue(releases.all { it.langTypes.size == 1 })

        val episodeMapping3 = createEpisodeMapping(anime, 1, 3, now)

        createEpisodeVariant(
            episodeMapping3,
            Platform.CRUN,
            "ja-JP",
            "test-3-CRUN-JA-JP",
            "test-1-CRUN.mp4",
            now
        )

        episodeVariantService.preIndex()
        InvalidationService.invalidateAll()

        weeklyReleases = animeService.getWeeklyAnimes(CountryCode.FR, null, now.toLocalDate().atStartOfWeek())
        releases = weeklyReleases.flatMap { it.releases }
        releases.forEach(::println)
        assertEquals(1, releases.size)
        assertTrue(releases.all { it.langTypes.size == 1 })
    }

    @Test
    fun getWeeklyAnimes_shouldGroupMultipleLangTypes_whenReleasedWithSmallDelay() {
        val now = ZonedDateTime.parse("2025-07-06T00:30:00Z[UTC]").withZoneSameInstant(Constant.utcZoneId)
        val anime = createAnime("TO BE HERO X")

        val episodeMapping1 = createEpisodeMapping(anime, 1, 14, now)

        createEpisodeVariant(
            episodeMapping1,
            Platform.CRUN,
            "ja-JP",
            "FR-CRUN-GX9U34KWP-JA-JP",
            "https://www.crunchyroll.com/fr/watch/GX9U34KWP/impromptu-counterattack",
            now
        )

        createEpisodeVariant(
            episodeMapping1,
            Platform.CRUN,
            "zn-CN",
            "FR-CRUN-GPWU8X0VN-ZH-CN",
            "https://www.crunchyroll.com/fr/watch/GPWU8X0VN/impromptu-counterattack",
            now
        )

        createEpisodeVariant(
            episodeMapping1,
            Platform.CRUN,
            "fr-FR",
            "FR-CRUN-GK9UGNQK1-FR-FR",
            "https://www.crunchyroll.com/fr/watch/GK9UGNQK1/",
            now.plusMinutes(15)
        )

        episodeVariantService.preIndex()
        InvalidationService.invalidateAll()

        val weeklyReleases = animeService.getWeeklyAnimes(CountryCode.FR, null, now.toLocalDate().atStartOfWeek())
        val releases = weeklyReleases.flatMap { it.releases }
        assertEquals(1, releases.size)
    }

    private fun createAnime(
        name: String,
        slug: String = StringUtils.toSlug(StringUtils.getShortName(name)),
        countryCode: CountryCode = CountryCode.FR
    ) = animeService.save(
        Anime(
            countryCode = countryCode,
            name = name,
            slug = slug
        )
    )

    private fun createEpisodeMapping(
        anime: Anime,
        season: Int,
        number: Int,
        releaseDateTime: ZonedDateTime,
        episodeType: EpisodeType = EpisodeType.EPISODE
    ) = episodeMappingService.save(
        EpisodeMapping(
            anime = anime,
            episodeType = episodeType,
            season = season,
            number = number,
            releaseDateTime = releaseDateTime
        )
    )

    private fun createEpisodeVariant(
        mapping: EpisodeMapping,
        platform: Platform,
        audioLocale: String,
        identifier: String,
        url: String,
        releaseDateTime: ZonedDateTime
    ) = episodeVariantService.save(
        EpisodeVariant(
            mapping = mapping,
            platform = platform,
            audioLocale = audioLocale,
            identifier = identifier,
            url = url,
            releaseDateTime = releaseDateTime
        )
    )
}