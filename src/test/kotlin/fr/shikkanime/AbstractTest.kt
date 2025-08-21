package fr.shikkanime

import com.google.inject.Inject
import fr.shikkanime.services.*
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.Database
import fr.shikkanime.utils.InvalidationService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

abstract class AbstractTest {
    @Inject protected lateinit var animeService: AnimeService
    @Inject protected lateinit var episodeMappingService: EpisodeMappingService
    @Inject protected lateinit var episodeVariantService: EpisodeVariantService
    @Inject protected lateinit var memberService: MemberService
    @Inject protected lateinit var memberActionService: MemberActionService
    @Inject protected lateinit var memberFollowAnimeService: MemberFollowAnimeService
    @Inject protected lateinit var memberFollowEpisodeService: MemberFollowEpisodeService
    @Inject protected lateinit var animePlatformService: AnimePlatformService
    @Inject protected lateinit var configService: ConfigService
    @Inject protected lateinit var traceActionService: TraceActionService
    @Inject protected lateinit var ruleService: RuleService
    @Inject protected lateinit var attachmentService: AttachmentService
    @Inject protected lateinit var database: Database

    @BeforeEach
    open fun setUp() {
        Constant.injector.injectMembers(this)
        InvalidationService.invalidateAll()
    }

    @AfterEach
    open fun tearDown() {
        attachmentService.clearPool()
        database.truncate()
        database.clearCache()
        InvalidationService.invalidateAll()
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            System.setProperty("isTest", "true")
        }
    }
}