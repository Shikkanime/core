package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.indexers.GroupedIndexer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class GroupedEpisodeRepositoryTest : AbstractTest() {
    @Inject private lateinit var groupedEpisodeService: GroupedEpisodeService

    @BeforeEach
    override fun setUp() {
        super.setUp()
        GroupedIndexer.clear()
    }

    @Test
    fun `findAllBy with searchTypes should filter variants within groups`() {
        val anime = Anime(countryCode = CountryCode.FR, name = "Test Anime", slug = "test-anime")
        animeService.save(anime)

        val now = ZonedDateTime.parse("2024-01-01T00:00:00Z")
        val mapping = EpisodeMapping(
            anime = anime,
            episodeType = EpisodeType.EPISODE,
            season = 1,
            number = 1,
            releaseDateTime = now,
        )
        episodeMappingService.save(mapping)

        val variantSub = EpisodeVariant(
            mapping = mapping,
            platform = Platform.CRUN,
            audioLocale = "ja-JP",
            identifier = "test-sub",
            url = "https://test.com/sub",
            releaseDateTime = now
        )
        episodeVariantService.save(variantSub)

        val variantVoice = EpisodeVariant(
            mapping = mapping,
            platform = Platform.CRUN,
            audioLocale = "fr-FR",
            identifier = "test-voice",
            url = "https://test.com/voice",
            releaseDateTime = now.plusMinutes(1)
        )
        episodeVariantService.save(variantVoice)

        // Force indexing
        GroupedIndexer.add(
            GroupedIndexer.CompositeKey(CountryCode.FR, anime.uuid!!, anime.slug!!, EpisodeType.EPISODE),
            variantSub.uuid!!,
            mapping.uuid!!,
            variantSub.releaseDateTime,
            variantSub.audioLocale!!
        )
        GroupedIndexer.add(
            GroupedIndexer.CompositeKey(CountryCode.FR, anime.uuid, anime.slug!!, EpisodeType.EPISODE),
            variantVoice.uuid!!,
            mapping.uuid,
            variantVoice.releaseDateTime,
            variantVoice.audioLocale!!
        )

        // 1. Search for SUBTITLES only
        val resultSub = groupedEpisodeService.findAllBy(CountryCode.FR, arrayOf(LangType.SUBTITLES), emptyList(), 1, 10)
        assertEquals(1, resultSub.data.size)
        val groupedEpisodeSub = resultSub.data.first()
        assertEquals(1, groupedEpisodeSub.variants.size)
        assertEquals("ja-JP", groupedEpisodeSub.variants.first().audioLocale)

        // 2. Search for VOICE only
        val resultVoice = groupedEpisodeService.findAllBy(CountryCode.FR, arrayOf(LangType.VOICE), emptyList(), 1, 10)
        assertEquals(1, resultVoice.data.size)
        val groupedEpisodeVoice = resultVoice.data.first()
        assertEquals(1, groupedEpisodeVoice.variants.size)
        assertEquals("fr-FR", groupedEpisodeVoice.variants.first().audioLocale)

        // 3. Search for both
        val resultBoth = groupedEpisodeService.findAllBy(
            CountryCode.FR,
            arrayOf(LangType.SUBTITLES, LangType.VOICE),
            emptyList(),
            1,
            10
        )
        assertEquals(1, resultBoth.data.size)
        assertEquals(2, resultBoth.data.first().variants.size)
    }
}
