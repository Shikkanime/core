package fr.shikkanime.services

import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.atStartOfWeek
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import kotlin.test.assertTrue

class AnimeServiceTest : AbstractTest() {
    @Test
    fun `getWeekly multiple lang types on same hour`() {
        val now = ZonedDateTime.now().withHour(19).withMinute(30).withSecond(0).withNano(0)
        val previousWeek = now.minusWeeks(1)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "A",
                slug = "a"
            )
        )

        val episodeMapping1 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1,
                releaseDateTime = previousWeek
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping1,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "test-1-CRUN-JA-JP",
                url = "test-1-CRUN.mp4",
                releaseDateTime = previousWeek
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping1,
                platform = Platform.CRUN,
                audioLocale = "fr-FR",
                identifier = "test-1-CRUN-FR-FR",
                url = "test-1-CRUN.mp4",
                releaseDateTime = previousWeek.plusMinutes(10)
            )
        )

        val episodeMapping2 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 2,
                releaseDateTime = now
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping2,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "test-2-CRUN-JA-JP",
                url = "test-1-CRUN.mp4",
                releaseDateTime = now
            )
        )

        MapCache.invalidateAll()

        var weeklyReleases = animeService.getWeeklyAnimes(CountryCode.FR, null, now.toLocalDate().atStartOfWeek())
        var releases = weeklyReleases.flatMap { it.releases }
        releases.forEach { println(it) }
        assertEquals(1, releases.size)
        assertEquals(1, releases.first().langTypes.size)

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping2,
                platform = Platform.CRUN,
                audioLocale = "fr-FR",
                identifier = "test-2-CRUN-FR-FR",
                url = "test-1-CRUN.mp4",
                releaseDateTime = now.plusMinutes(10)
            )
        )

        MapCache.invalidateAll()

        weeklyReleases = animeService.getWeeklyAnimes(CountryCode.FR, null, now.toLocalDate().atStartOfWeek())
        releases = weeklyReleases.flatMap { it.releases }
        releases.forEach { println(it) }
        assertEquals(1, releases.size)
        assertEquals(2, releases.first().langTypes.size)
    }

    @Test
    fun `getWeekly multiple lang types on different hour`() {
        val now = ZonedDateTime.now().withHour(17).withMinute(0).withSecond(0).withNano(0)
        val previousWeek = now.minusWeeks(1)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "A",
                slug = "a"
            )
        )

        val episodeMapping1 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 2,
                releaseDateTime = previousWeek
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping1,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "test-2-CRUN-JA-JP",
                url = "test-1-CRUN.mp4",
                releaseDateTime = previousWeek
            )
        )

        val episodeMapping2 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1,
                releaseDateTime = previousWeek.plusHours(6)
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping2,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "test-1-CRUN-JA-JP",
                url = "test-1-CRUN.mp4",
                releaseDateTime = previousWeek.plusHours(6)
            )
        )

        MapCache.invalidateAll()

        var weeklyReleases = animeService.getWeeklyAnimes(CountryCode.FR, null, now.toLocalDate().atStartOfWeek())
        var releases = weeklyReleases.flatMap { it.releases }
        assertEquals(2, releases.size)
        assertTrue(releases.all { it.langTypes.size == 1 })

        val episodeMapping3 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 3,
                releaseDateTime = now
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping3,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "test-3-CRUN-JA-JP",
                url = "test-1-CRUN.mp4",
                releaseDateTime = now
            )
        )

        MapCache.invalidateAll()

        weeklyReleases = animeService.getWeeklyAnimes(CountryCode.FR, null, now.toLocalDate().atStartOfWeek())
        releases = weeklyReleases.flatMap { it.releases }
        releases.forEach { println(it) }
        assertEquals(1, releases.size)
        assertTrue(releases.all { it.langTypes.size == 1 })
    }
}