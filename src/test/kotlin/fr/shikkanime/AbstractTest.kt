package fr.shikkanime

import com.google.inject.Inject
import fr.shikkanime.entities.ShikkEntity
import fr.shikkanime.repositories.AbstractRepository
import fr.shikkanime.services.*
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.Database
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

abstract class AbstractTest {
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

    @Inject
    protected lateinit var ruleService: RuleService

    @Inject
    protected lateinit var attachmentService: AttachmentService

    @Inject
    private lateinit var database: Database

    @BeforeEach
    open fun setUp() {
        Constant.injector.injectMembers(this)
        MapCache.invalidateAll()
    }

    private fun deleteAll() {
        database.entityManager.use { entityManager ->
            val transaction = entityManager.transaction
            transaction.begin()

            val services = Constant.reflections.getSubTypesOf(AbstractService::class.java)
                .map { klass -> Constant.injector.getInstance(klass) }

            val serviceEntityCounts = services.associateWith { service ->
                val repository = service.javaClass.getDeclaredMethod("getRepository").apply { isAccessible = true }.invoke(service) as AbstractRepository<*>
                val entityClass = repository.javaClass.getDeclaredMethod("getEntityClass").apply { isAccessible = true }.invoke(repository) as Class<*>
                entityClass.declaredFields.count { field ->
                    field.isAccessible = true
                    field.type.superclass != null && ShikkEntity::class.java.isAssignableFrom(field.type.superclass)
                }.toLong()
            }

            serviceEntityCounts.toList()
                .sortedByDescending { it.second }
                .forEach { (service, _) -> service.deleteAll(entityManager) }

            transaction.commit()
        }
    }

    @AfterEach
    open fun tearDown() {
        deleteAll()
        attachmentService.clearPool()
        database.clearCache()
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            System.setProperty("isTest", "true")
        }
    }
}