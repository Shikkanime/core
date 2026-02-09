package fr.shikkanime.factories.impl

import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.StringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class GroupedEpisodeFactoryTest {

    private val factory = GroupedEpisodeFactory()

    private fun createAnime(name: String): Anime {
        return Anime(
            uuid = UUID.randomUUID(),
            countryCode = CountryCode.FR,
            name = name,
            releaseDateTime = ZonedDateTime.now(),
            slug = StringUtils.toSlug(StringUtils.getShortName(name))
        )
    }

    private fun createMapping(
        anime: Anime,
        season: Int,
        number: Int,
        episodeType: EpisodeType = EpisodeType.EPISODE
    ): EpisodeMapping {
        return EpisodeMapping(
            uuid = UUID.randomUUID(),
            anime = anime,
            releaseDateTime = ZonedDateTime.now(),
            episodeType = episodeType,
            season = season,
            number = number
        )
    }

    private fun createVariant(
        mapping: EpisodeMapping,
        audioLocale: String,
        platform: Platform = Platform.CRUN
    ): EpisodeVariant {
        return EpisodeVariant(
            uuid = UUID.randomUUID(),
            mapping = mapping,
            releaseDateTime = ZonedDateTime.now(),
            platform = platform,
            audioLocale = audioLocale,
            identifier = UUID.randomUUID().toString(),
            url = "https://example.com"
        )
    }

    /**
     * Test case 1: Frieren S2 EP1-4 (1 VF and 4 VOSTFR)
     * Expected: 2 groups - [EP1 VF+VOSTFR] and [EP2-4 VOSTFR]
     */
    @Test
    fun `Frieren S2 EP1-4 with 1 VF and 4 VOSTFR should create 2 groups`() {
        val anime = createAnime("Frieren")

        // Create mappings for EP1-4
        val mappingEp1 = createMapping(anime, 2, 1)
        val mappingEp4 = createMapping(anime, 2, 4)

        // Create variants: EP1 has both VF and VOSTFR, EP2-4 only VOSTFR
        val variants = listOf(
            createVariant(mappingEp1, "fr-FR"),  // VF EP1
            createVariant(mappingEp4, "ja-JP"),  // VOSTFR EP4
        )

        val groups = factory.toEntities(variants)

        assertEquals(2, groups.size, "Should have 2 groups")

        // Find the group with EP1 (should have both VF and VOSTFR)
        val ep1Group = groups.find { it.minNumber == 1 && it.maxNumber == 1 }
        assertEquals(1, ep1Group?.variants?.size, "EP1 group should have 1 variants (VF)")

        // Find the group with EP4 (should have only VOSTFR)
        val ep4Group = groups.find { it.minNumber == 4 }
        assertEquals(4, ep4Group?.maxNumber, "EP group should end at number 4")
        assertTrue(ep4Group?.variants?.all { it.audioLocale == "ja-JP" } == true, "EP4 should all be VOSTFR")
    }

    /**
     * Test case 2: Frieren S4 VOSTFR & VF (same episode)
     * Expected: 1 group with both languages
     */
    @Test
    fun `Frieren S4 with same episode in VOSTFR and VF should create 1 group`() {
        val anime = createAnime("Frieren")

        val mapping = createMapping(anime, 4, 1)

        val variants = listOf(
            createVariant(mapping, "fr-FR"),  // VF
            createVariant(mapping, "ja-JP"),  // VOSTFR
        )

        val groups = factory.toEntities(variants)

        assertEquals(1, groups.size, "Should have 1 group")
        assertEquals(2, groups[0].variants.size, "Group should have 2 variants (VF + VOSTFR)")
    }

    /**
     * Test case 3: Evangelion S1 EP1-12 VOSTFR (all episodes at once)
     * Expected: 1 group
     */
    @Test
    fun `Evangelion S1 EP1-12 VOSTFR should create 1 group`() {
        val anime = createAnime("Evangelion")

        val variants = (1..12).map { episodeNumber ->
            val mapping = createMapping(anime, 1, episodeNumber)
            createVariant(mapping, "ja-JP")  // VOSTFR
        }

        val groups = factory.toEntities(variants)

        assertEquals(1, groups.size, "Should have 1 group")
        assertEquals(12, groups[0].variants.size, "Group should have 12 variants")
        assertEquals(1, groups[0].minNumber, "Should start at EP1")
        assertEquals(12, groups[0].maxNumber, "Should end at EP12")
    }

    /**
     * Test case 4: Baki S1 EP1-12 VOSTFR & VF (all episodes at once)
     * Expected: 1 group with both languages for each episode
     */
    @Test
    fun `Baki S1 EP1-12 VOSTFR and VF should create 1 group`() {
        val anime = createAnime("Baki")

        val variants = (1..12).flatMap { episodeNumber ->
            val mapping = createMapping(anime, 1, episodeNumber)
            listOf(
                createVariant(mapping, "ja-JP"),  // VOSTFR
                createVariant(mapping, "fr-FR"),  // VF
            )
        }

        val groups = factory.toEntities(variants)

        assertEquals(1, groups.size, "Should have 1 group")
        assertEquals(24, groups[0].variants.size, "Group should have 24 variants (12 episodes x 2 languages)")
    }

    /**
     * Test case 5: Violet Evergarden S1-3 EP1-12 (all episodes from seasons 1, 2, and 3 at once)
     * Expected: 1 group
     */
    @Test
    fun `Violet Evergarden S1-3 EP1-12 should create 1 group`() {
        val anime = createAnime("Violet Evergarden")

        val variants = (1..3).flatMap { season ->
            (1..12).map { episodeNumber ->
                val mapping = createMapping(anime, season, episodeNumber)
                createVariant(mapping, "ja-JP")  // VOSTFR
            }
        }

        val groups = factory.toEntities(variants)

        assertEquals(1, groups.size, "Should have 1 group")
        assertEquals(36, groups[0].variants.size, "Group should have 36 variants (3 seasons x 12 episodes)")
        assertEquals(1, groups[0].minSeason, "Should start at S1")
        assertEquals(3, groups[0].maxSeason, "Should end at S3")
    }

    /**
     * Test: Non-consecutive episodes should be in separate groups
     */
    @Test
    fun `Non-consecutive episodes should be in separate groups`() {
        val anime = createAnime("Test Anime")

        val mappingEp1 = createMapping(anime, 1, 1)
        val mappingEp5 = createMapping(anime, 1, 5)
        val mappingEp10 = createMapping(anime, 1, 10)

        val variants = listOf(
            createVariant(mappingEp1, "ja-JP"),
            createVariant(mappingEp5, "ja-JP"),
            createVariant(mappingEp10, "ja-JP"),
        )

        val groups = factory.toEntities(variants)

        assertEquals(3, groups.size, "Should have 3 separate groups for non-consecutive episodes")
    }

    /**
     * Test: Different episode types should be in separate groups
     */
    @Test
    fun `Different episode types should be in separate groups`() {
        val anime = createAnime("Test Anime")

        val episodeMapping = createMapping(anime, 1, 1, EpisodeType.EPISODE)
        val specialMapping = createMapping(anime, 1, 1, EpisodeType.SPECIAL)

        val variants = listOf(
            createVariant(episodeMapping, "ja-JP"),
            createVariant(specialMapping, "ja-JP"),
        )

        val groups = factory.toEntities(variants)

        assertEquals(2, groups.size, "Should have 2 separate groups for different episode types")
    }

    /**
     * Test: Different animes should be in separate groups
     */
    @Test
    fun `Different animes should be in separate groups`() {
        val anime1 = createAnime("Anime 1")
        val anime2 = createAnime("Anime 2")

        val mapping1 = createMapping(anime1, 1, 1)
        val mapping2 = createMapping(anime2, 1, 1)

        val variants = listOf(
            createVariant(mapping1, "ja-JP"),
            createVariant(mapping2, "ja-JP"),
        )

        val groups = factory.toEntities(variants)

        assertEquals(2, groups.size, "Should have 2 separate groups for different animes")
    }

    /**
     * Test: Empty list should return empty result
     */
    @Test
    fun `Empty list should return empty result`() {
        val groups = factory.toEntities(emptyList())
        assertTrue(groups.isEmpty(), "Empty input should return empty result")
    }

    /**
     * Test: Complex scenario - Multiple VF episodes and more VOSTFR episodes
     * Example: VF EP1-2, VOSTFR EP1-5
     * Expected: 2 groups - [EP1-2 VF+VOSTFR] and [EP3-5 VOSTFR]
     */
    @Test
    fun `VF EP1-2 and VOSTFR EP1-5 should create 2 groups`() {
        val anime = createAnime("Complex Anime")

        val mappingEp1 = createMapping(anime, 1, 1)
        val mappingEp2 = createMapping(anime, 1, 2)
        val mappingEp3 = createMapping(anime, 1, 3)
        val mappingEp4 = createMapping(anime, 1, 4)
        val mappingEp5 = createMapping(anime, 1, 5)

        val variants = listOf(
            createVariant(mappingEp1, "fr-FR"),  // VF EP1
            createVariant(mappingEp2, "fr-FR"),  // VF EP2
            createVariant(mappingEp1, "ja-JP"),  // VOSTFR EP1
            createVariant(mappingEp2, "ja-JP"),  // VOSTFR EP2
            createVariant(mappingEp3, "ja-JP"),  // VOSTFR EP3
            createVariant(mappingEp4, "ja-JP"),  // VOSTFR EP4
            createVariant(mappingEp5, "ja-JP"),  // VOSTFR EP5
        )

        val groups = factory.toEntities(variants)

        assertEquals(2, groups.size, "Should have 2 groups")

        // Find the group with common episodes (EP1-2 with both languages)
        val commonGroup = groups.find { it.variants.size == 4 && it.variants.any { v -> v.audioLocale == "fr-FR" } }
        assertEquals(4, commonGroup?.variants?.size, "Common group should have 4 variants (EP1-2 x 2 languages)")

        // Find the group with VOSTFR only (EP3-5)
        val vostfrOnlyGroup = groups.find { it.variants.all { v -> v.audioLocale == "ja-JP" } && it.variants.size == 3 }
        assertEquals(3, vostfrOnlyGroup?.variants?.size, "VOSTFR only group should have 3 variants (EP3-5)")
    }

    /**
     * Test: Season transition - S1 EP12 followed by S2 EP1 should be consecutive
     */
    @Test
    fun `Season transition S1 EP12 to S2 EP1 should be consecutive`() {
        val anime = createAnime("Season Transition Anime")

        val mappingS1Ep12 = createMapping(anime, 1, 12)
        val mappingS2Ep1 = createMapping(anime, 2, 1)

        val variants = listOf(
            createVariant(mappingS1Ep12, "ja-JP"),
            createVariant(mappingS2Ep1, "ja-JP"),
        )

        val groups = factory.toEntities(variants)

        assertEquals(1, groups.size, "Season transition should be treated as consecutive")
        assertEquals(2, groups[0].variants.size, "Group should contain both episodes")
    }
}
