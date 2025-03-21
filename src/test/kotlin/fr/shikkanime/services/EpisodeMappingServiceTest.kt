package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.utils.ObjectParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EpisodeMappingServiceTest : AbstractTest() {
    @Inject
    private lateinit var episodeMappingFactory: EpisodeMappingFactory

    @Test
    fun update() {
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Test Anime",
                image = "test.jpg",
                banner = "test.jpg",
                slug = "test-anime"
            )
        )
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.SPECIAL,
                season = 1,
                number = 1,
                image = "test.jpg",
            )
        )

        val dto = episodeMappingFactory.toDto(episodeMapping)
        dto.episodeType = EpisodeType.FILM

        val updated = episodeMappingService.update(episodeMapping.uuid!!, dto)
        assertEquals(EpisodeType.FILM, updated!!.episodeType)
    }

    @Test
    fun `update with different identifier`() {
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Test Anime",
                image = "test.jpg",
                banner = "test.jpg",
                slug = "test-anime"
            )
        )
        episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.FILM,
                season = 1,
                number = 1,
                image = "test.jpg",
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.SPECIAL,
                season = 1,
                number = 1,
                image = "test.jpg",
            )
        )

        val dto = episodeMappingFactory.toDto(episodeMapping)
        dto.episodeType = EpisodeType.FILM

        val updated = episodeMappingService.update(episodeMapping.uuid!!, dto)
        assertNotEquals(updated!!.uuid, episodeMapping.uuid)
        assertEquals(EpisodeType.FILM, updated.episodeType)
        assertNull(episodeMappingService.find(episodeMapping.uuid))
    }

    @Test
    fun `global update`() {
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Oshi no Ko",
                slug = "oshi-no-ko",
                image = "https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/affiche_350x500.jpg",
                banner = "https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/license_640x360.jpg",
                description = "Gorô Amemiya, un obstétricien exerçant dans un hôpital de campagne, n’a d’yeux que pour la sublime Aï Hoshino, une célèbre et talentueuse idole. Au détour d’une rencontre fortuite, la découverte d’un terrible secret va changer leur destin à tout jamais…"
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.EPISODE,
                season = 2,
                number = 17,
                image = "https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/eps17_640x360.jpg",
                title = "Grandir",
                description = "La première de « Tokyo Blade » commence sous le regard inquisiteur de Kichijôji. Conscient de ses lacunes et du fossé qui le sépare du reste du casting, Melt parviendra-t-il à la faire changer d'opinion ?",
                duration = 1530,
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-25963-JA-JP",
                url = "https://animationdigitalnetwork.fr/video/oshi-no-ko/25963-episode-17-grandir",
            )
        )

        val dto = ObjectParser.fromJson(
            "{\"uuid\":\"212f0b36-f76b-48ad-8bd3-214ff4052f45\",\"anime\":{\"audioLocales\":[\"fr-FR\",\"ja-JP\"],\"langTypes\":[\"SUBTITLES\",\"VOICE\"],\"seasons\":[{\"number\":1,\"lastReleaseDateTime\":\"2023-06-28T15:00:00Z\"},{\"number\":2,\"lastReleaseDateTime\":\"2024-08-07T15:00:00Z\"}],\"status\":\"VALID\",\"uuid\":\"a07c197c-067a-4b19-8fed-38dbf3f9f989\",\"countryCode\":\"FR\",\"name\":\"Oshi no Ko\",\"shortName\":\"Oshi no Ko\",\"slug\":\"oshi-no-ko\",\"releaseDateTime\":\"2023-04-12T15:00:00Z\",\"lastReleaseDateTime\":\"2024-08-07T15:00:00Z\",\"image\":\"https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/affiche_350x500.jpg\",\"banner\":\"https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/license_640x360.jpg\",\"description\":\"Gorô Amemiya, un obstétricien exerçant dans un hôpital de campagne, n’a d’yeux que pour la sublime Aï Hoshino, une célèbre et talentueuse idole. Au détour d’une rencontre fortuite, la découverte d’un terrible secret va changer leur destin à tout jamais…\"},\"releaseDateTime\":\"2024-08-07T15:00:00Z\",\"lastReleaseDateTime\":\"2024-08-07T15:00:00Z\",\"lastUpdateDateTime\":\"2024-08-07T15:00:00Z\",\"episodeType\":\"EPISODE\",\"season\":2,\"number\":17,\"duration\":1530,\"title\":\"Grandir\",\"description\":\"La première de « Tokyo Blade » commence sous le regard inquisiteur de Kichijôji. Conscient de ses lacunes et du fossé qui le sépare du reste du casting, Melt parviendra-t-il à la faire changer d'opinion ?\",\"image\":\"https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/eps17_640x360.jpg\",\"variants\":[{\"uuid\":\"85c4a481-189b-4710-bef2-49677dd32a65\",\"releaseDateTime\":\"2024-08-07T15:00:00Z\",\"platform\":{\"id\":\"ANIM\",\"name\":\"Animation Digital Network\",\"url\":\"https://animationdigitalnetwork.fr/\",\"image\":\"animation_digital_network.jpg\"},\"audioLocale\":\"ja-JP\",\"identifier\":\"FR-ANIM-25963-JA-JP\",\"url\":\"https://animationdigitalnetwork.fr/video/oshi-no-ko/25963-episode-17-grandir\",\"uncensored\":false}],\"platforms\":[{\"id\":\"ANIM\",\"name\":\"Animation Digital Network\",\"url\":\"https://animationdigitalnetwork.fr/\",\"image\":\"animation_digital_network.jpg\"}],\"langTypes\":[\"SUBTITLES\"],\"status\":\"VALID\"}",
            EpisodeMappingDto::class.java
        )
        println(dto)
        episodeMappingService.update(episodeMapping.uuid!!, dto)
    }
}