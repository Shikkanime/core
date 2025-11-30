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
    fun `should delete variants not included in DTO`() {
        // Arrange
        val anime = createTestAnime()
        val episodeMapping = createEpisodeMapping(
            anime = anime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 1
        )

        // Create three variants
        val variant1 = episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-1-JA-JP",
                url = "https://example.com/variant1",
            )
        )

        val variant2 = episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                platform = Platform.CRUN,
                audioLocale = "en-US",
                identifier = "FR-CRUN-2-EN-US",
                url = "https://example.com/variant2",
            )
        )

        val variant3 = episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                platform = Platform.NETF,
                audioLocale = "fr-FR",
                identifier = "FR-NETF-3-FR-FR",
                url = "https://example.com/variant3",
            )
        )

        // Create DTO with only the first and third variants
        val originalDto = episodeMappingFactory.toDto(episodeMapping)

        // Create a new DTO with filtered variants
        val dto = EpisodeMappingDto(
            uuid = originalDto.uuid,
            anime = originalDto.anime,
            releaseDateTime = originalDto.releaseDateTime,
            lastReleaseDateTime = originalDto.lastReleaseDateTime,
            lastUpdateDateTime = originalDto.lastUpdateDateTime,
            episodeType = originalDto.episodeType,
            season = originalDto.season,
            number = originalDto.number,
            duration = originalDto.duration,
            title = originalDto.title,
            description = originalDto.description,
            variants = originalDto.variants?.filter { it.uuid != variant2.uuid }?.toSet(),
            image = originalDto.image,
            sources = emptySet()
        )

        // Act
        val updatedMapping = episodeMappingAdminService.update(episodeMapping.uuid!!, dto)

        // Assert
        assertNotNull(updatedMapping)

        // Get all variants for the updated mapping
        val remainingVariants = episodeVariantService.findAllByMapping(updatedMapping!!)

        // Should have only 2 variants
        assertEquals(2, remainingVariants.size)

        // Should contain variant1 and variant3, but not variant2
        assertTrue(remainingVariants.any { it.uuid == variant1.uuid })
        assertFalse(remainingVariants.any { it.uuid == variant2.uuid })
        assertTrue(remainingVariants.any { it.uuid == variant3.uuid })
    }

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
            "{\"uuid\":\"212f0b36-f76b-48ad-8bd3-214ff4052f45\",\"anime\":{\"audioLocales\":[\"fr-FR\",\"ja-JP\"],\"langTypes\":[\"SUBTITLES\",\"VOICE\"],\"seasons\":[{\"number\":1,\"releaseDateTime\":\"2023-06-28T15:00:00Z\",\"lastReleaseDateTime\":\"2023-06-28T15:00:00Z\",\"episodes\":0},{\"number\":2,\"releaseDateTime\":\"2023-06-28T15:00:00Z\",\"lastReleaseDateTime\":\"2023-06-28T15:00:00Z\",\"episodes\":0}],\"status\":\"VALID\",\"uuid\":\"a07c197c-067a-4b19-8fed-38dbf3f9f989\",\"countryCode\":\"FR\",\"name\":\"Oshi no Ko\",\"shortName\":\"Oshi no Ko\",\"slug\":\"oshi-no-ko\",\"releaseDateTime\":\"2023-04-12T15:00:00Z\",\"lastReleaseDateTime\":\"2024-08-07T15:00:00Z\",\"image\":\"https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/affiche_350x500.jpg\",\"banner\":\"https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/license_640x360.jpg\",\"description\":\"Gorô Amemiya, un obstétricien exerçant dans un hôpital de campagne, n'a d'yeux que pour la sublime Aï Hoshino, une célèbre et talentueuse idole. Au détour d'une rencontre fortuite, la découverte d'un terrible secret va changer leur destin à tout jamais…\"},\"releaseDateTime\":\"2024-08-07T15:00:00Z\",\"lastReleaseDateTime\":\"2024-08-07T15:00:00Z\",\"lastUpdateDateTime\":\"2024-08-07T15:00:00Z\",\"episodeType\":\"EPISODE\",\"season\":2,\"number\":17,\"duration\":1530,\"title\":\"Grandir\",\"description\":\"La première de « Tokyo Blade » commence sous le regard inquisiteur de Kichijôji. Conscient de ses lacunes et du fossé qui le sépare du reste du casting, Melt parviendra-t-il à la faire changer d'opinion ?\",\"image\":\"https://image.animationdigitalnetwork.fr/license/oshinoko/tv/web/eps17_640x360.jpg\",\"variants\":[{\"uuid\":\"85c4a481-189b-4710-bef2-49677dd32a65\",\"releaseDateTime\":\"2024-08-07T15:00:00Z\",\"platform\":{\"id\":\"ANIM\",\"name\":\"Animation Digital Network\",\"url\":\"https://animationdigitalnetwork.fr/\",\"image\":\"animation_digital_network.jpg\"},\"audioLocale\":\"ja-JP\",\"identifier\":\"FR-ANIM-25963-JA-JP\",\"url\":\"https://animationdigitalnetwork.fr/video/oshi-no-ko/25963-episode-17-grandir\",\"uncensored\":false}],\"platforms\":[{\"id\":\"ANIM\",\"name\":\"Animation Digital Network\",\"url\":\"https://animationdigitalnetwork.fr/\",\"image\":\"animation_digital_network.jpg\"}],\"langTypes\":[\"SUBTITLES\"],\"status\":\"VALID\"}",
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

    @Test
    fun `should create anime when migrating episode to non-existing anime`() {
        // Arrange
        val sourceAnime = createTestAnime(name = "Source Anime", slug = "source-anime")
        val episodeMapping = createEpisodeMapping(
            anime = sourceAnime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 1
        )

        val dto = episodeMappingFactory.toDto(episodeMapping)
        dto.anime!!.name = "New Anime Name"

        // Act
        val updatedMapping = episodeMappingAdminService.update(episodeMapping.uuid!!, dto)

        // Assert
        assertNotNull(updatedMapping)
        assertEquals("New Anime Name", updatedMapping!!.anime!!.name)
        
        // Check that the new anime was created by querying it separately
        val newAnime = animeService.findByName(sourceAnime.countryCode!!, "New Anime Name")
        assertNotNull(newAnime)
        assertEquals("new-name", newAnime!!.slug)
        assertEquals(sourceAnime.countryCode, newAnime.countryCode)
        assertEquals(sourceAnime.description, newAnime.description)
    }

    @Test
    fun `should migrate episodes in batch to existing anime`() {
        // Arrange
        val sourceAnime = createTestAnime(name = "Source Anime", slug = "source-anime")
        val targetAnime = createTestAnime(name = "Target Anime", slug = "target-anime")
        
        val episode1 = createEpisodeMapping(
            anime = sourceAnime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 1
        )
        
        val episode2 = createEpisodeMapping(
            anime = sourceAnime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 2
        )

        val updateDto = fr.shikkanime.dtos.mappings.UpdateAllEpisodeMappingDto(
            uuids = setOf(episode1.uuid!!, episode2.uuid!!),
            episodeType = null,
            season = null,
            animeName = targetAnime.name,
            startDate = null,
            incrementDate = null,
            bindVoiceVariants = null,
            forceUpdate = null,
            bindNumber = null
        )

        // Act
        episodeMappingAdminService.updateAll(updateDto)

        // Assert
        val updatedEpisode1 = episodeMappingService.find(episode1.uuid)
        val updatedEpisode2 = episodeMappingService.find(episode2.uuid)
        
        assertNotNull(updatedEpisode1)
        assertNotNull(updatedEpisode2)
        assertEquals(targetAnime.uuid, updatedEpisode1!!.anime!!.uuid)
        assertEquals(targetAnime.uuid, updatedEpisode2!!.anime!!.uuid)
    }

    @Test
    fun `should migrate episodes in batch and create new anime if not exists`() {
        // Arrange
        val sourceAnime = createTestAnime(name = "Source Anime", slug = "source-anime")
        
        val episode1 = createEpisodeMapping(
            anime = sourceAnime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 1
        )
        
        val episode2 = createEpisodeMapping(
            anime = sourceAnime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 2
        )

        val updateDto = fr.shikkanime.dtos.mappings.UpdateAllEpisodeMappingDto(
            uuids = setOf(episode1.uuid!!, episode2.uuid!!),
            episodeType = null,
            season = null,
            animeName = "Brand New Anime",
            startDate = null,
            incrementDate = null,
            bindVoiceVariants = null,
            forceUpdate = null,
            bindNumber = null
        )

        // Act
        episodeMappingAdminService.updateAll(updateDto)

        // Assert
        val updatedEpisode1 = episodeMappingService.find(episode1.uuid)
        val updatedEpisode2 = episodeMappingService.find(episode2.uuid)
        
        assertNotNull(updatedEpisode1)
        assertNotNull(updatedEpisode2)
        
        // Check that the new anime was created by querying it separately
        val newAnime = animeService.findByName(sourceAnime.countryCode!!, "Brand New Anime")
        assertNotNull(newAnime)
        assertEquals("brand-new-anime", newAnime!!.slug)
        assertEquals(sourceAnime.countryCode, newAnime.countryCode)
        assertEquals(sourceAnime.description, newAnime.description)
        assertEquals("Brand New Anime", newAnime.name)
        
        // Verify episodes are now associated with the new anime
        assertEquals(newAnime.uuid, updatedEpisode1!!.anime!!.uuid)
        assertEquals(newAnime.uuid, updatedEpisode2!!.anime!!.uuid)
    }

    @Test
    fun `should merge episodes when migrating to anime with existing episode`() {
        // Arrange
        val sourceAnime = createTestAnime(name = "Source Anime", slug = "source-anime")
        val targetAnime = createTestAnime(name = "Target Anime", slug = "target-anime")
        
        // Create episode in source anime
        val sourceEpisode = createEpisodeMapping(
            anime = sourceAnime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 1
        )
        
        // Create episode with same season/type/number in target anime
        val targetEpisode = createEpisodeMapping(
            anime = targetAnime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 1
        )

        // Add variants to both episodes
        episodeVariantService.save(
            EpisodeVariant(
                mapping = sourceEpisode,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "SOURCE-VARIANT",
                url = "https://example.com/source",
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = targetEpisode,
                platform = Platform.CRUN,
                audioLocale = "en-US",
                identifier = "TARGET-VARIANT",
                url = "https://example.com/target",
            )
        )

        val updateDto = fr.shikkanime.dtos.mappings.UpdateAllEpisodeMappingDto(
            uuids = setOf(sourceEpisode.uuid!!),
            episodeType = null,
            season = null,
            animeName = targetAnime.name,
            startDate = null,
            incrementDate = null,
            bindVoiceVariants = null,
            forceUpdate = null,
            bindNumber = null
        )

        // Act
        episodeMappingAdminService.updateAll(updateDto)

        // Assert
        // Source episode should be deleted
        assertNull(episodeMappingService.find(sourceEpisode.uuid))
        
        // Target episode should still exist and have both variants
        val updatedTargetEpisode = episodeMappingService.find(targetEpisode.uuid!!)
        assertNotNull(updatedTargetEpisode)
        
        val variants = episodeVariantService.findAllByMapping(updatedTargetEpisode!!)
        assertEquals(2, variants.size)
        assertTrue(variants.any { it.url == "https://example.com/source" })
        assertTrue(variants.any { it.url == "https://example.com/target" })
    }

    @Test
    fun `should cleanup empty source anime after migration`() {
        // Arrange
        val sourceAnime = createTestAnime(name = "Source Anime", slug = "source-anime")
        val targetAnime = createTestAnime(name = "Target Anime", slug = "target-anime")
        
        // Create only one episode in source anime
        val episode = createEpisodeMapping(
            anime = sourceAnime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 1
        )

        val updateDto = fr.shikkanime.dtos.mappings.UpdateAllEpisodeMappingDto(
            uuids = setOf(episode.uuid!!),
            episodeType = null,
            season = null,
            animeName = targetAnime.name,
            startDate = null,
            incrementDate = null,
            bindVoiceVariants = null,
            forceUpdate = null,
            bindNumber = null
        )

        // Act
        episodeMappingAdminService.updateAll(updateDto)

        // Assert
        // Source anime should be deleted since it has no more episodes
        assertNull(animeService.find(sourceAnime.uuid!!))
        
        // Target anime should still exist
        assertNotNull(animeService.find(targetAnime.uuid!!))
        
        // Episode should be in target anime
        val updatedEpisode = episodeMappingService.find(episode.uuid)
        assertNotNull(updatedEpisode)
        assertEquals(targetAnime.uuid, updatedEpisode!!.anime!!.uuid)
    }

    @Test
    fun `should not cleanup source anime if it still has episodes after migration`() {
        // Arrange
        val sourceAnime = createTestAnime(name = "Source Anime", slug = "source-anime")
        val targetAnime = createTestAnime(name = "Target Anime", slug = "target-anime")
        
        // Create two episodes in source anime
        val episode1 = createEpisodeMapping(
            anime = sourceAnime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 1
        )
        
        val episode2 = createEpisodeMapping(
            anime = sourceAnime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 2
        )

        // Only migrate one episode
        val updateDto = fr.shikkanime.dtos.mappings.UpdateAllEpisodeMappingDto(
            uuids = setOf(episode1.uuid!!),
            episodeType = null,
            season = null,
            animeName = targetAnime.name,
            startDate = null,
            incrementDate = null,
            bindVoiceVariants = null,
            forceUpdate = null,
            bindNumber = null
        )

        // Act
        episodeMappingAdminService.updateAll(updateDto)

        // Assert
        // Source anime should still exist since it has one remaining episode
        assertNotNull(animeService.find(sourceAnime.uuid!!))
        
        // Episode2 should still be in source anime
        val remainingEpisode = episodeMappingService.find(episode2.uuid!!)
        assertNotNull(remainingEpisode)
        assertEquals(sourceAnime.uuid, remainingEpisode!!.anime!!.uuid)
        
        // Episode1 should be in target anime
        val migratedEpisode = episodeMappingService.find(episode1.uuid)
        assertNotNull(migratedEpisode)
        assertEquals(targetAnime.uuid, migratedEpisode!!.anime!!.uuid)
    }

    @Test
    fun `should handle batch migration with mixed scenarios`() {
        // Arrange
        val sourceAnime1 = createTestAnime(name = "Source Anime 1", slug = "source-anime-1")
        val sourceAnime2 = createTestAnime(name = "Source Anime 2", slug = "source-anime-2")
        
        val episode1 = createEpisodeMapping(
            anime = sourceAnime1,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 1
        )
        
        val episode2 = createEpisodeMapping(
            anime = sourceAnime2,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 2
        )

        val updateDto = fr.shikkanime.dtos.mappings.UpdateAllEpisodeMappingDto(
            uuids = setOf(episode1.uuid!!, episode2.uuid!!),
            episodeType = null,
            season = null,
            animeName = "Unified Anime",
            startDate = null,
            incrementDate = null,
            bindVoiceVariants = null,
            forceUpdate = null,
            bindNumber = null
        )

        // Act
        episodeMappingAdminService.updateAll(updateDto)

        // Assert
        val updatedEpisode1 = episodeMappingService.find(episode1.uuid)
        val updatedEpisode2 = episodeMappingService.find(episode2.uuid)
        
        assertNotNull(updatedEpisode1)
        assertNotNull(updatedEpisode2)
        
        // Check that the unified anime was created
        val unifiedAnime = animeService.findByName(sourceAnime1.countryCode!!, "Unified Anime")
        assertNotNull(unifiedAnime)
        assertEquals("unified-anime", unifiedAnime!!.slug)
        assertEquals("Unified Anime", unifiedAnime.name)
        
        // Verify episodes are now associated with the unified anime
        assertEquals(unifiedAnime.uuid, updatedEpisode1!!.anime!!.uuid)
        assertEquals(unifiedAnime.uuid, updatedEpisode2!!.anime!!.uuid)
        
        // Both source animes should be deleted
        assertNull(animeService.find(sourceAnime1.uuid!!))
        assertNull(animeService.find(sourceAnime2.uuid!!))
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
