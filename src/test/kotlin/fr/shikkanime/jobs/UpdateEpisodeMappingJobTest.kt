package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.*
import fr.shikkanime.utils.InvalidationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZonedDateTime
import java.util.stream.Stream

class UpdateEpisodeMappingJobTest : AbstractTest() {
    @Inject private lateinit var updateEpisodeMappingJob: UpdateEpisodeMappingJob

    @BeforeEach
    override fun setUp() {
        super.setUp()

        configService.save(
            Config(
                propertyKey = ConfigPropertyKey.CHECK_PREVIOUS_AND_NEXT_EPISODES.key,
                propertyValue = "true"
            )
        )

        InvalidationService.invalidate(Config::class.java)
    }

    data class TestCase(
        val animeName: String,
        val slug: String,
        val season: Int,
        val episodeType: EpisodeType,
        val episodeNumber: Int,
        val platforms: List<PlatformData>,
        val episodeTitle: String? = null,
        val episodeDescription: String? = null,
        val expectedMappingsCount: Int,
        val expectedVariantsCount: Int,
        val expectedBannerUrl: String? = null,
        val expectedUpdatedTitle: String? = null,
        val checkPreviousEpisode: Boolean = false,
        val previousAndNextDepth: Int = 1
    )

    data class PlatformData(
        val platform: Platform,
        val audioLocale: String,
        val identifier: String,
        val url: String
    )

    companion object {
        @JvmStatic
        fun testCases(): Stream<TestCase> = Stream.of(
            TestCase(
                animeName = "Rent-a-Girlfriend",
                slug = "rent-a-girlfriend",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 1,
                platforms = listOf(
                    PlatformData(
                        platform = Platform.CRUN,
                        audioLocale = "fr-FR",
                        identifier = "FR-CRUN-GZ7UV8KWZ-FR-FR",
                        url = "https://www.crunchyroll.com/fr/watch/GZ7UV8KWZ/rent-a-girlfriend"
                    )
                ),
                expectedMappingsCount = 2,
                expectedVariantsCount = 3,
                expectedBannerUrl = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/97ab10f90157c828a591cd4ec66e851c.jpg",
                expectedUpdatedTitle = "Petite amie à louer"
            ),
            TestCase(
                animeName = "The Eminence in Shadow",
                slug = "the-eminence-in-shadow",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 1,
                platforms = listOf(
                    PlatformData(
                        platform = Platform.ANIM,
                        audioLocale = "ja-JP",
                        identifier = "FR-ANIM-20568-JA-JP",
                        url = "https://animationdigitalnetwork.fr/video/the-eminence-in-shadow/20568-episode-1-un-camarade-detestable"
                    )
                ),
                expectedMappingsCount = 2,
                expectedVariantsCount = 4
            ),
            TestCase(
                animeName = "Pon no Michi",
                slug = "pon-no-michi",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 5,
                episodeTitle = "Une arrivée inattendue",
                episodeDescription = "Nashiko et ses amies reçoivent la visite de la personne contre qui elles jouaient en ligne : Haneru Emi. Cette dernière est déterminée à jouer à nouveau contre Riche pour la battre, mais avant ça, elle fait la connaissance des quatre filles. Et quoi de mieux pour cela qu'un barbecue sous le ciel bleu ?",
                platforms = listOf(
                    PlatformData(
                        platform = Platform.ANIM,
                        audioLocale = "ja-JP",
                        identifier = "FR-ANIM-24026-JA-JP",
                        url = "https://animationdigitalnetwork.fr/video/pon-no-michi/24026-episode-5-une-arrivee-inattendue"
                    )
                ),
                expectedMappingsCount = 3,
                expectedVariantsCount = 3
            ),
            TestCase(
                animeName = "Berserk : L'Âge d'or - Memorial Edition",
                slug = "berserk",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 4,
                platforms = listOf(
                    PlatformData(
                        platform = Platform.CRUN,
                        audioLocale = "ja-JP",
                        identifier = "FR-CRUN-GJWU23V97-JA-JP",
                        url = "https://www.crunchyroll.com/fr/watch/GJWU23V97/prepared-for-death"
                    ),
                    PlatformData(
                        platform = Platform.CRUN,
                        audioLocale = "fr-FR",
                        identifier = "FR-CRUN-G50UZQEW0-FR-FR",
                        url = "https://www.crunchyroll.com/fr/watch/G50UZQEW0/prepared-for-death"
                    )
                ),
                expectedMappingsCount = 3,
                expectedVariantsCount = 8
            ),
            TestCase(
                animeName = "Isekai Cheat Magician",
                slug = "isekai-cheat-magician",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 13,
                episodeTitle = "La Nuit aux étoiles",
                episodeDescription = "Taichi, Rin, Remia et Myûra sont de retour à Azpire pour participer à un festival en mémoire des morts.",
                platforms = listOf(
                    PlatformData(
                        platform = Platform.ANIM,
                        audioLocale = "ja-JP",
                        identifier = "FR-ANIM-10114-JA-JP",
                        url = "https://animationdigitalnetwork.fr/video/isekai-cheat-magician/10114-episode-13-la-nuit-aux-etoiles"
                    )
                ),
                expectedMappingsCount = 2,
                expectedVariantsCount = 2
            ),
            TestCase(
                animeName = "Let's Make a Mug Too",
                slug = "lets-make-a-mug-too",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 9,
                episodeTitle = "Écraser, étirer, reculer, compléter",
                episodeDescription = "Himeno n'a toujours pas trouvé l'idée ultime pour le concours. Elle essaie donc plusieurs formes pour créer le coussin qu'elle désire.",
                platforms = listOf(
                    PlatformData(
                        platform = Platform.CRUN,
                        audioLocale = "ja-JP",
                        identifier = "FR-CRUN-GK9U383WX-JA-JP",
                        url = "https://www.crunchyroll.com/fr/watch/GK9U383WX/pound-stretch-subtract-and-add"
                    )
                ),
                expectedMappingsCount = 3,
                expectedVariantsCount = 3
            ),
            TestCase(
                animeName = "One Piece",
                slug = "one-piece",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 816,
                episodeTitle = "La Nuit aux étoiles",
                episodeDescription = "Taichi, Rin, Remia et Myûra sont de retour à Azpire pour participer à un festival en mémoire des morts.",
                platforms = listOf(
                    PlatformData(
                        platform = Platform.ANIM,
                        audioLocale = "ja-JP",
                        identifier = "FR-ANIM-16948-JA-JP",
                        url = "https://animationdigitalnetwork.fr/video/isekai-cheat-magician/10114-episode-13-la-nuit-aux-etoiles"
                    )
                ),
                expectedMappingsCount = 3,
                expectedVariantsCount = 6
            ),
            TestCase(
                animeName = "One Piece",
                slug = "one-piece",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 566,
                episodeTitle = "Conclusion. L'affrontement final contre Hody !",
                platforms = listOf(
                    PlatformData(
                        platform = Platform.ANIM,
                        audioLocale = "fr-FR",
                        identifier = "FR-ANIM-13530-FR-FR",
                        url = "https://animationdigitalnetwork.fr/video/one-piece-saga-8-ile-des-hommes-poissons/13530-episode-566-conclusion-l-affrontement-final-contre-hody"
                    )
                ),
                expectedMappingsCount = 3,
                expectedVariantsCount = 6,
                checkPreviousEpisode = true,
            ),
            TestCase(
                animeName = "DAN DA DAN",
                slug = "dan-da-dan",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 1,
                platforms = listOf(
                    PlatformData(
                        platform = Platform.NETF,
                        audioLocale = "ja-JP",
                        identifier = "FR-NETF-a7b9feca-JA-JP",
                        url = "https://www.netflix.com/fr/title/81736884"
                    ),
                    PlatformData(
                        platform = Platform.ANIM,
                        audioLocale = "fr-FR",
                        identifier = "FR-ANIM-26662-FR-FR",
                        url = "https://animationdigitalnetwork.fr/video/dan-da-dan/26662-episode-1-serait-ce-une-romance-qui-commence"
                    ),
                    PlatformData(
                        platform = Platform.ANIM,
                        audioLocale = "ja-JP",
                        identifier = "FR-ANIM-26662-JA-JP",
                        url = "https://animationdigitalnetwork.fr/video/dan-da-dan/26662-episode-1-serait-ce-une-romance-qui-commence"
                    ),
                    PlatformData(
                        platform = Platform.CRUN,
                        audioLocale = "ja-JP",
                        identifier = "FR-CRUN-GN7UNXWMJ-JA-JP",
                        url = "https://www.crunchyroll.com/fr/watch/GN7UNXWMJ/thats-how-love-starts-ya-know"
                    ),
                    PlatformData(
                        platform = Platform.NETF,
                        audioLocale = "fr-FR",
                        identifier = "FR-NETF-a7b9feca-FR-FR",
                        url = "https://www.netflix.com/fr/title/81736884"
                    ),
                    PlatformData(
                        platform = Platform.CRUN,
                        audioLocale = "fr-FR",
                        identifier = "FR-CRUN-GG1UXWE24-FR-FR",
                        url = "https://www.crunchyroll.com/fr/watch/GG1UXWE24/"
                    )
                ),
                expectedMappingsCount = 3,
                expectedVariantsCount = 10
            ),
            TestCase(
                animeName = "Garôden : La voie du loup solitaire",
                slug = "garoden",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 7,
                platforms = listOf(
                    PlatformData(
                        platform = Platform.NETF,
                        audioLocale = "ja-JP",
                        identifier = "FR-NETF-6c247a4d-JA-JP",
                        url = "https://www.netflix.com/fr/title/6c247a4d"
                    )
                ),
                expectedMappingsCount = 1,
                expectedVariantsCount = 1
            ),
            TestCase(
                animeName = "Du mouvement de la Terre",
                slug = "du-mouvement-de-la-terre",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 9,
                platforms = listOf(
                    PlatformData(
                        platform = Platform.NETF,
                        audioLocale = "ja-JP",
                        identifier = "FR-NETF-6936fd55-JA-JP",
                        url = "https://www.netflix.com/fr/title/81765022"
                    )
                ),
                expectedMappingsCount = 1,
                expectedVariantsCount = 1
            ),
            TestCase(
                animeName = "Ninja Kamui",
                slug = "ninja-kamui",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 10,
                platforms = listOf(
                    PlatformData(
                        platform = Platform.PRIM,
                        audioLocale = "ja-JP",
                        identifier = "FR-PRIM-0bdc4c77-JA-JP",
                        url = "https://www.primevideo.com/-/fr/detail/0QN9ZXJ935YBTNK8U9FV5OAX5B"
                    )
                ),
                expectedMappingsCount = 1,
                expectedVariantsCount = 1
            ),
            TestCase(
                animeName = "given",
                slug = "given",
                season = 1,
                episodeType = EpisodeType.FILM,
                episodeNumber = 2,
                platforms = listOf(
                    PlatformData(
                        platform = Platform.CRUN,
                        audioLocale = "ja-JP",
                        identifier = "FR-CRUN-GMKUEMXMJ-JA-JP",
                        url = "https://www.crunchyroll.com/fr/watch/GMKUEMXMJ/given-the-movie-hiiragi-mix"
                    )
                ),
                expectedMappingsCount = 5,
                expectedVariantsCount = 5,
                previousAndNextDepth = 3
            ),
            TestCase(
                animeName = "ZENSHU",
                slug = "zenshu",
                season = 1,
                episodeType = EpisodeType.EPISODE,
                episodeNumber = 1,
                platforms = listOf(
                    PlatformData(
                        platform = Platform.CRUN,
                        audioLocale = "ja-JP",
                        identifier = "FR-CRUN-GZ7UD3DKN-JA-JP",
                        url = "https://www.crunchyroll.com/fr/watch/GZ7UD3DKN/"
                    )
                ),
                expectedMappingsCount = 2,
                expectedVariantsCount = 4
            ),
        )
    }

    @ParameterizedTest(name = "Testing {0}")
    @MethodSource("testCases")
    fun `should update episode mappings`(testCase: TestCase) {
        configService.save(
            Config(
                propertyKey = ConfigPropertyKey.PREVIOUS_NEXT_EPISODES_DEPTH.key,
                propertyValue = testCase.previousAndNextDepth.toString()
            )
        )

        InvalidationService.invalidate(Config::class.java)

        val zonedDateTime = ZonedDateTime.now().minusMonths(2)
        
        // Create anime
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = testCase.animeName,
                slug = testCase.slug,
            )
        )

        // Create episode mapping
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = testCase.season,
                episodeType = testCase.episodeType,
                number = testCase.episodeNumber,
                title = testCase.episodeTitle,
                description = testCase.episodeDescription
            )
        )

        // Create episode variants
        testCase.platforms.forEach { platformData ->
            episodeVariantService.save(
                EpisodeVariant(
                    mapping = episodeMapping,
                    releaseDateTime = zonedDateTime,
                    platform = platformData.platform,
                    audioLocale = platformData.audioLocale,
                    identifier = platformData.identifier,
                    url = platformData.url
                )
            )
        }

        InvalidationService.invalidateAll()

        // Run the job
        updateEpisodeMappingJob.run()

        // Verify results
        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        
        val mappings = episodeMappingService.findAll()
        assertEquals(testCase.expectedMappingsCount, mappings.size)
        
        val variants = episodeVariantService.findAll()
        assertEquals(testCase.expectedVariantsCount, variants.size)
        
        // Verify updated metadata if expected
        if (testCase.expectedBannerUrl != null) {
            val banner = attachmentService.findByEntityUuidTypeAndActive(mappings.first().uuid!!, ImageType.BANNER)
            assertEquals(testCase.expectedBannerUrl, banner?.url)
        }
        
        if (testCase.expectedUpdatedTitle != null) {
            assertEquals(testCase.expectedUpdatedTitle, mappings.first().title)
        }
        
        // Check previous episode if needed
        if (testCase.checkPreviousEpisode) {
            val previousEpisode = episodeMappingService.findAllByAnime(anime)
                .find { it.number == testCase.episodeNumber - 1 }
            assertNotNull(previousEpisode)
            
            val previousEpisodeVariants = episodeVariantService.findAllByMapping(previousEpisode!!)
            assertTrue(previousEpisodeVariants.isNotEmpty())
        }
    }
}