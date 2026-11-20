package fr.shikkanime.jobs.metadata

import fr.shikkanime.database.services.*
import fr.shikkanime.models.*
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

/**
 * End-to-end matching test: the clients answer with the real API payloads captured as
 * fixtures, and the linker must persist the right links - the whole point of the feature.
 */
class MetadataLinkerTest {
    private val animeService = mockk<AnimeService>()
    private val episodeService = mockk<EpisodeService>()
    private val animeLinkService = mockk<AnimeMetadataLinkService>()
    private val episodeLinkService = mockk<EpisodeMetadataLinkService>()

    private val cache = MetadataApiCache(mockk<ApiCacheService>(relaxed = true))
    private val anilistClient = AniListClient(cache)
    private val malClient = MyAnimeListClient(cache)
    private val anidbClient = AniDbClient(cache)

    private val anilistMock = mockk<AniListClient>()
    private val malMock = mockk<MyAnimeListClient>()
    private val anidbMock = mockk<AniDbClient>()

    private lateinit var storedAnime: AnimeWithChildren
    private lateinit var storedEpisodes: List<EpisodeWithChildren>

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResourceAsStream("/metadata/$name"))
            .bufferedReader().readText()

    @BeforeEach
    fun setUp() {
        val animeId = Uuid.random()
        storedAnime = AnimeWithChildren(
            anime = Anime(animeId, null, null, "Sgt. Frog"),
            images = emptyList(),
            localizations = listOf(
                AnimeLocalization(
                    null, null, null, animeId, Locale.FR_FR,
                    "Sergent Keroro", "Une grenouille qui veut conquérir la Terre."
                )
            ),
            platforms = emptyList()
        )
        storedEpisodes = (1..3).map { number ->
            EpisodeWithChildren(
                episode = Episode(
                    Uuid.random(), null, null, animeId,
                    season = 1, episodeType = EpisodeType.EPISODE, number = number,
                    releaseDateTime = LocalDateTime(2004, 4, 3, 0, 0), duration = 0
                ),
                images = emptyList(), localizations = emptyList(), releases = emptyList()
            )
        }

        // Clients answer with the real captured payloads
        val anilistMedia = cache.decode<AniListPageResponse>(
            fixture("anilist_search_sergeant_keroro.json")
        ).data!!.page!!.media!!
        coEvery { anilistMock.searchAnime(any()) } returns
            anilistMedia.map { anilistClient.run { it.toExternalAnime() } }
        coEvery { anilistMock.getAnimeByIds(any()) } returns emptyList()

        val malSearch = cache.decode<MyAnimeListSearchResponse>(fixture("mal_search_keroro.json"))
        coEvery { malMock.searchAnime(any()) } returns
            malSearch.data.map { malClient.run { it.toExternalAnime() } }
        coEvery { malMock.getAnimeById(any()) } returns null
        coEvery { malMock.getAnimeById(516) } returns
            malClient.run {
                cache.decode<MyAnimeListMediaResponse>(fixture("mal_anime_516.json"))
                    .data!!.toExternalAnime()
            }
        coEvery { malMock.getEpisodesByMediaId(any()) } returns emptyList()
        coEvery { malMock.getEpisodesByMediaId(516) } returns
            cache.decode<MyAnimeListEpisodeResponse>(fixture("mal_episodes_516_p1.json"))
                .data.mapIndexed { index, episode ->
                    ExternalEpisode(
                        id = "516/${episode.id}", number = index + 1,
                        title = episode.title, airDate = episode.aired?.take(10)?.let(LocalDate::parse)
                    )
                }

        coEvery { anidbMock.searchAnime(any()) } returns listOf(
            ExternalAnime(
                platform = MetadataPlatform.ANIDB, id = "1552",
                titles = setOf("Sgt. Frog", "Keroro Gunsou", "Sergeant Keroro"),
                description = null, episodeCount = null, year = null, episodes = emptyList()
            )
        )
        coEvery { anidbMock.getAnimeByIds(any()) } returns emptyList()
        coEvery { anidbMock.getAnimeByIds(listOf(1552)) } returns listOf(
            anidbClient.parseAnimeDocument(fixture("anidb_anime_1552.xml"), 1552)!!
        )

        every { animeService.findAll() } returns listOf(storedAnime)
        every { episodeService.findAllByAnimeId(any()) } returns storedEpisodes
        every { animeLinkService.save(any()) } answers { firstArg() }
        every { episodeLinkService.save(any()) } answers { firstArg() }
    }

    private fun linker() = MetadataLinker(
        animeService, episodeService, animeLinkService, episodeLinkService,
        anilistMock, malMock, anidbMock
    )

    @Test
    fun `links the anime to the right entry of every platform`() {
        // When
        val report = runBlocking { linker().linkAll() }

        // Then
        assertEquals(1, report.animesProcessed)
        verify {
            animeLinkService.save(match {
                it.platform == MetadataPlatform.ANILIST && it.platformId == "516"
            })
        }
        verify {
            animeLinkService.save(match {
                it.platform == MetadataPlatform.ANIDB && it.platformId == "1552"
            })
        }
        verify {
            animeLinkService.save(match {
                it.platform == MetadataPlatform.MYANIMELIST && it.platformId == "516"
            })
        }
    }

    @Test
    fun `franchise reference pins link the anime but never its episodes`() {
        // Major is pinned by hand: seasons 1 to 5 are references owning no stored season,
        // season 6 owns the stored episodes through the -129 numbering offset.
        val animeId = Uuid.random()
        val major = AnimeWithChildren(
            anime = Anime(animeId, null, null, "Major"),
            images = emptyList(), localizations = emptyList(), platforms = emptyList()
        )
        val episodes = listOf(
            EpisodeWithChildren(
                episode = Episode(
                    Uuid.random(), null, null, animeId,
                    season = 6, episodeType = EpisodeType.EPISODE, number = 130,
                    releaseDateTime = LocalDateTime(2010, 4, 3, 0, 0), duration = 0
                ),
                images = emptyList(), localizations = emptyList(), releases = emptyList()
            )
        )
        every { animeService.findAll() } returns listOf(major)
        every { episodeService.findAllByAnimeId(animeId) } returns episodes

        coEvery { malMock.getAnimeById(7655) } returns ExternalAnime(
            platform = MetadataPlatform.MYANIMELIST, id = "7655", titles = emptySet(),
            description = null, episodeCount = null, year = null, episodes = emptyList()
        )
        coEvery { malMock.getEpisodesByMediaId(7655) } returns listOf(
            ExternalEpisode(id = "7655/1", number = 1, title = null, airDate = null)
        )
        coEvery { anidbMock.getAnimeByIds(listOf(7236)) } returns emptyList()

        // When
        runBlocking { linker().linkAll() }

        // Then: six entries per platform, references included
        verify(exactly = 18) { animeLinkService.save(any()) }
        verify {
            animeLinkService.save(match {
                it.platform == MetadataPlatform.ANILIST && it.platformId == "627"
            })
        }
        verify {
            animeLinkService.save(match {
                it.platform == MetadataPlatform.ANIDB && it.platformId == "2168"
            })
        }
        // Only the season-6 pin produces an episode link, through the offset
        verify(exactly = 1) { episodeLinkService.save(any()) }
        verify {
            episodeLinkService.save(match {
                it.platform == MetadataPlatform.MYANIMELIST &&
                    it.platformId == "7655/1" && it.platformAnimeId == "7655"
            })
        }
    }

    @Test
    fun `links every stored episode to its external episode`() {
        // When
        runBlocking { linker().linkAll() }

        // Then: 3 episodes x 2 episode-capable platforms
        verify(exactly = 6) { episodeLinkService.save(any()) }
        verify {
            episodeLinkService.save(match {
                it.platform == MetadataPlatform.MYANIMELIST &&
                    it.platformId == "516/1" && it.platformAnimeId == "516"
            })
        }
        verify {
            episodeLinkService.save(match {
                it.platform == MetadataPlatform.ANIDB && it.platformAnimeId == "1552"
            })
        }
    }
}
