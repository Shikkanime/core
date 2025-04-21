package fr.shikkanime.services.admin

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.dtos.mappings.EpisodeMappingDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.factories.impl.EpisodeMappingFactory
import fr.shikkanime.utils.ObjectParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EpisodeMappingAdminServiceTest : AbstractTest() {
    @Inject private lateinit var episodeMappingFactory: EpisodeMappingFactory
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var episodeMappingAdminService: EpisodeMappingAdminService

    @Test
    fun `should update episode mapping type`() {
        // Arrange
        val anime = createTestAnime()
        val episodeMapping = createEpisodeMapping(
            anime = anime,
            episodeType = EpisodeType.SPECIAL,
            season = 1,
            number = 1
        )

        val dto = episodeMappingFactory.toDto(episodeMapping)
        dto.episodeType = EpisodeType.FILM

        // Act
        val updatedMapping = episodeMappingAdminService.update(episodeMapping.uuid!!, dto)

        // Assert
        assertNotNull(updatedMapping)
        assertEquals(EpisodeType.FILM, updatedMapping!!.episodeType)
    }

    @Test
    fun `should create new mapping when updating with conflicting identifier`() {
        // Arrange
        val anime = createTestAnime()
        
        // Create first mapping with FILM type
        episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = EpisodeType.FILM,
                season = 1,
                number = 1,
            )
        )

        // Create second mapping with SPECIAL type
        val originalMapping = createEpisodeMapping(
            anime = anime,
            episodeType = EpisodeType.SPECIAL,
            season = 1,
            number = 1,
        )

        val dto = episodeMappingFactory.toDto(originalMapping)
        dto.episodeType = EpisodeType.FILM

        // Act
        val updatedMapping = episodeMappingAdminService.update(originalMapping.uuid!!, dto)

        // Assert
        assertNotNull(updatedMapping)
        assertNotEquals(updatedMapping!!.uuid, originalMapping.uuid)
        assertEquals(EpisodeType.FILM, updatedMapping.episodeType)
        assertNull(episodeMappingService.find(originalMapping.uuid))
    }

    @Test
    fun `should update mapping with complex json data`() {
        // Arrange
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Oshi no Ko",
                slug = "oshi-no-ko",
                description = "Gorô Amemiya, un obstétricien exerçant dans un hôpital de campagne, n'a d'yeux que pour la sublime Aï Hoshino, une célèbre et talentueuse idole. Au détour d'une rencontre fortuite, la découverte d'un terrible secret va changer leur destin à tout jamais…"
            )
        )

        val episodeMapping = createEpisodeMapping(
            anime = anime,
            episodeType = EpisodeType.EPISODE,
            season = 2,
            number = 17,
            title = "Grandir",
            description = "La première de « Tokyo Blade » commence sous le regard inquisiteur de Kichijôji. Conscient de ses lacunes et du fossé qui le sépare du reste du casting, Melt parviendra-t-il à la faire changer d'opinion ?",
            duration = 1530
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

        val complexDto = ObjectParser.fromJson(
            "{\"uuid\":\"212f0b36-f76b-48ad-8bd3-214ff4052f45\",\"anime\":{\"audioLocales\":[\"fr-FR\",\"ja-JP\"],\"langTypes\":[\"SUBTITLES\",\"VOICE\"],\"seasons\":[{\"number\":1,\"lastReleaseDateTime\":\"2023-06-28T15:00:00Z\"},{\"number\":2,\"lastReleaseDateTime\":\"2024-08-07T15:00:00Z\"}],\"status\":\"VALID\",\"uuid\":\"a07c197c-067a-4b19-8fed-38dbf3f9f989\",\"countryCode\":\"FR\",\"name\":\"Oshi no Ko\",\"shortName\":\"Oshi no Ko\",\"slug\":\"oshi-no-ko\",\"releaseDateTime\":\"2023-04-12T15:00:00Z\",\"lastReleaseDateTime\":\"2024-08-07T15:00:00Z\",\"image\":\"https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/affiche_350x500.jpg\",\"banner\":\"https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/license_640x360.jpg\",\"description\":\"Gorô Amemiya, un obstétricien exerçant dans un hôpital de campagne, n'a d'yeux que pour la sublime Aï Hoshino, une célèbre et talentueuse idole. Au détour d'une rencontre fortuite, la découverte d'un terrible secret va changer leur destin à tout jamais…\"},\"releaseDateTime\":\"2024-08-07T15:00:00Z\",\"lastReleaseDateTime\":\"2024-08-07T15:00:00Z\",\"lastUpdateDateTime\":\"2024-08-07T15:00:00Z\",\"episodeType\":\"EPISODE\",\"season\":2,\"number\":17,\"duration\":1530,\"title\":\"Grandir\",\"description\":\"La première de « Tokyo Blade » commence sous le regard inquisiteur de Kichijôji. Conscient de ses lacunes et du fossé qui le sépare du reste du casting, Melt parviendra-t-il à la faire changer d'opinion ?\",\"image\":\"https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/eps17_640x360.jpg\",\"variants\":[{\"uuid\":\"85c4a481-189b-4710-bef2-49677dd32a65\",\"releaseDateTime\":\"2024-08-07T15:00:00Z\",\"platform\":{\"id\":\"ANIM\",\"name\":\"Animation Digital Network\",\"url\":\"https://animationdigitalnetwork.fr/\",\"image\":\"animation_digital_network.jpg\"},\"audioLocale\":\"ja-JP\",\"identifier\":\"FR-ANIM-25963-JA-JP\",\"url\":\"https://animationdigitalnetwork.fr/video/oshi-no-ko/25963-episode-17-grandir\",\"uncensored\":false}],\"platforms\":[{\"id\":\"ANIM\",\"name\":\"Animation Digital Network\",\"url\":\"https://animationdigitalnetwork.fr/\",\"image\":\"animation_digital_network.jpg\"}],\"langTypes\":[\"SUBTITLES\"],\"status\":\"VALID\"}",
            EpisodeMappingDto::class.java
        )

        // Act
        val updatedMapping = episodeMappingAdminService.update(episodeMapping.uuid!!, complexDto)

        // Assert
        assertNotNull(updatedMapping)
    }

    @Test
    fun `should update episode mapping with different anime`() {
        // Arrange
        val originalAnime = createTestAnime(name = "Test Anime", slug = "test-anime")
        val newAnime = createTestAnime(name = "Test Anime 2", slug = "test-anime-2")

        val episodeMapping = createEpisodeMapping(
            anime = originalAnime,
            episodeType = EpisodeType.SPECIAL,
            season = 1,
            number = 1
        )

        val dto = episodeMappingFactory.toDto(episodeMapping)
        dto.anime = animeFactory.toDto(newAnime)

        // Act
        val updatedMapping = episodeMappingAdminService.update(episodeMapping.uuid!!, dto)

        // Assert
        assertNotNull(updatedMapping)
        assertEquals(updatedMapping!!.uuid, episodeMapping.uuid)
        assertNotEquals(updatedMapping.anime!!.uuid, originalAnime.uuid)
        assertEquals(updatedMapping.anime!!.uuid, newAnime.uuid)
        assertEquals(EpisodeType.SPECIAL, updatedMapping.episodeType)
    }

    // Helper methods
    private fun createTestAnime(
        name: String = "Test Anime",
        slug: String = "test-anime"
    ): Anime {
        return animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = name,
                slug = slug
            )
        )
    }

    private fun createEpisodeMapping(
        anime: Anime,
        episodeType: EpisodeType,
        season: Int,
        number: Int,
        title: String? = null,
        description: String? = null,
        duration: Long = -1
    ): EpisodeMapping {
        return episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                episodeType = episodeType,
                season = season,
                number = number,
                title = title,
                description = description,
                duration = duration
            )
        )
    }
}