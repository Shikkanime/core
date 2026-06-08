package fr.shikkanime.jobs

import fr.shikkanime.builders.*
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.DisneyPlusPlatform
import fr.shikkanime.services.*
import fr.shikkanime.services.caches.ConfigCacheService
import fr.shikkanime.services.caches.EpisodeVariantCacheService
import fr.shikkanime.wrappers.impl.DisneyPlusWrapper
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

@Suppress("UNUSED_PARAMETER")
class UpdateEpisodeJobMockTest {
    @InjectMockKs private lateinit var updateEpisodeJob: UpdateEpisodeJob
    @RelaxedMockK private lateinit var configCacheService: ConfigCacheService
    @RelaxedMockK private lateinit var animeService: AnimeService
    @RelaxedMockK private lateinit var episodeMappingService: EpisodeMappingService
    @RelaxedMockK private lateinit var episodeVariantService: EpisodeVariantService
    @RelaxedMockK private lateinit var episodeVariantCacheService: EpisodeVariantCacheService
    @RelaxedMockK private lateinit var attachmentService: AttachmentService
    @RelaxedMockK private lateinit var animePlatformService: AnimePlatformService
    @RelaxedMockK private lateinit var disneyPlusPlatform: DisneyPlusPlatform
    @RelaxedMockK private lateinit var mailService: MailService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DisneyPlusWrapper)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    suspend fun `should create anime platform when run with missing anime platform`() {
        // Given
        val animeUuid = UUID.randomUUID()
        val platform = Platform.DISN

        val anime = AnimeMockBuilder().apply {
            uuid = animeUuid
            countryCode = CountryCode.FR
            name = "Star Wars: Vision"
        }.build()

        val episodeMapping = EpisodeMappingMockBuilder().apply {
            uuid = UUID.randomUUID()
            this.anime = anime
            season = 1
            episodeType = EpisodeType.EPISODE
            number = 1
        }.build()

        val episodeVariant = EpisodeVariantMockBuilder().apply {
            uuid = UUID.randomUUID()
            mapping = episodeMapping
            this.platform = platform
            identifier = "FR-DISN-90b9a970-0ec1-4bbc-ad26-0639f3784f41-JA-JP"
            available = true
        }.build()

        val showPlatformId = "2bd9a5cd-ab23-40ec-b211-4c87f2e94528"

        val show = DisneyPlusShowMockBuilder().apply {
            id = showPlatformId
            name = "Star Wars: Vision"
            image = ""
            banner = ""
            carousel = ""
            title = ""
            seasons = setOf(
                "2dfb3f47-775a-407d-a993-c3605a42b5fd",
                "e65d16ac-49b3-40b7-bdd2-a31f092bfe40",
                "d568572b-c31c-45ac-8316-e326619ddfe0"
            )
        }.build()

        val episode = DisneyPlusEpisodeMockBuilder().apply {
            this.show = show
            id = "96342486-d67b-453a-a994-b0426cd390bc"
            seasonId = "2dfb3f47-775a-407d-a993-c3605a42b5fd"
            season = 1
            number = 1
            image = ""
            url = ""
            audioLocales = arrayOf("ja-JP")
        }.build()

        val episodeMetadata = DisneyPlusMetadataMockBuilder().apply {
            showId = show.id
            id = episode.id
        }.build()

        coEvery { episodeMappingService.findAllNeedUpdate() } returns listOf(episodeMapping)
        coEvery { configCacheService.getValueAsInt(ConfigPropertyKey.UPDATE_EPISODE_SIZE, any()) } returns 1
        coEvery { episodeVariantService.findAllByMapping(episodeMapping) } returns listOf(episodeVariant)
        coEvery { DisneyPlusWrapper.getMetadataByEpisodeId("90b9a970-0ec1-4bbc-ad26-0639f3784f41") } returns episodeMetadata
        coEvery { DisneyPlusWrapper.getEpisodesByShowId(any(), showPlatformId, any()) } returns arrayOf(episode)
        every { disneyPlusPlatform.getPlatform() } answers { callOriginal() }
        every { disneyPlusPlatform.convertEpisode(any(), any(), any()) } answers { callOriginal() }

        coEvery { animePlatformService.findByAnimePlatformAndId(animeUuid, platform, showPlatformId) } returns null
        coEvery { animeService.getReference(animeUuid) } returns anime
        val animePlatformSlot = slot<AnimePlatform>()
        coEvery { animePlatformService.save(capture(animePlatformSlot)) } answers { firstArg() }

        // When
        updateEpisodeJob.run()

        // Then
        coVerify(exactly = 1) { animePlatformService.save(any()) }
        assertEquals(platform, animePlatformSlot.captured.platform)
        assertEquals(episodeMetadata.showId, animePlatformSlot.captured.platformId)
    }
}