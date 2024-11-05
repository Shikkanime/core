package fr.shikkanime

import com.google.inject.Inject
import fr.shikkanime.services.*
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.Database
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

abstract class AbstractTest {
    @Inject
    private lateinit var database: Database

    @Inject
    protected lateinit var animeService: AnimeService

    @Inject
    protected lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    protected lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    protected lateinit var memberService: MemberService

    @Inject
    protected lateinit var memberActionService: MemberActionService

    @Inject
    protected lateinit var memberFollowAnimeService: MemberFollowAnimeService

    @Inject
    protected lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @Inject
    protected lateinit var animePlatformService: AnimePlatformService

    @Inject
    protected lateinit var configService: ConfigService

    @Inject
    protected lateinit var traceActionService: TraceActionService


    @BeforeEach
    open fun setUp() {
        Constant.injector.injectMembers(this)
        MapCache.invalidateAll()
    }

    private fun deleteAll() {
        database.entityManager.use {
            val transaction = it.transaction
            transaction.begin()

            Constant.reflections.getSubTypesOf(AbstractService::class.java).forEach {
                Constant.injector.getInstance(it).deleteAll()
            }

            transaction.commit()
        }
    }

    @AfterEach
    open fun tearDown() {
        deleteAll()
        ImageService.clearPool()
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            Constant.disableImageConversion = true
        }
    }
}