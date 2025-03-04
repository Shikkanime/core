package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.MapCache
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class UpdateEpisodeMappingJobTest : AbstractTest() {
    @Inject
    private lateinit var updateEpisodeMappingJob: UpdateEpisodeMappingJob

    @BeforeEach
    override fun setUp() {
        super.setUp()

        configService.save(
            Config(
                propertyKey = ConfigPropertyKey.CHECK_PREVIOUS_AND_NEXT_EPISODES.key,
                propertyValue = "true"
            )
        )

        MapCache.invalidate(Config::class.java)
        AbstractNetflixWrapper.checkLanguage = false
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        AbstractNetflixWrapper.checkLanguage = true
    }

    @Test
    fun `run old Crunchyroll episodes`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Rent-a-Girlfriend",
                slug = "rent-a-girlfriend",
                image = "test.jpg",
                banner = "test.jpg",
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 1,
                image = "test.jpg"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "fr-FR",
                identifier = "FR-CRUN-GZ7UV8KWZ-FR-FR",
                url = "https://www.crunchyroll.com/fr/watch/GZ7UV8KWZ/rent-a-girlfriend"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(2, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(3, variants.size)

        assertEquals(
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/97ab10f90157c828a591cd4ec66e851c.jpg",
            mappings.first().image
        )
        assertEquals("Petite amie à louer", mappings.first().title)
        assertEquals(
            "Kazuya Kinoshita est un jeune étudiant qui vient de se faire plaquer par sa copine. Alors qu'il déprime complètement, il décide de télécharger une application permettant de louer une petite amie pour une journée.",
            mappings.first().description
        )
    }

    @Test
    fun `run old ADN episodes`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "The Eminence in Shadow",
                slug = "the-eminence-in-shadow",
                image = "test.jpg",
                banner = "test.jpg"
            )
        )
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 1,
                image = "test.jpg",
            )
        )
        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-20568-JA-JP",
                url = "https://animationdigitalnetwork.fr/video/the-eminence-in-shadow/20568-episode-1-un-camarade-detestable"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(2, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(4, variants.size)
    }

    @Test
    fun `run old ADN episodes Pon No Michi`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Pon no Michi",
                slug = "pon-no-michi",
                image = "https://image.animationdigitalnetwork.fr/license/ponnomichi/tv/web/affiche_350x500.jpg",
                banner = "https://image.animationdigitalnetwork.fr/license/ponnomichi/tv/web/license_640x360.jpg"
            )
        )
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 5,
                image = "https://image.animationdigitalnetwork.fr/license/ponnomichi/tv/web/eps5_640x360.jpg",
                title = "Une arrivée inattendue",
                description = "Nashiko et ses amies reçoivent la visite de la personne contre qui elles jouaient en ligne : Haneru Emi. Cette dernière est déterminée à jouer à nouveau contre Riche pour la battre, mais avant ça, elle fait la connaissance des quatre filles. Et quoi de mieux pour cela qu’un barbecue sous le ciel bleu ?"
            )
        )
        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-24026-JA-JP",
                url = "https://animationdigitalnetwork.fr/video/pon-no-michi/24026-episode-5-une-arrivee-inattendue"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(3, mappings.size)
        assertTrue(mappings.first().lastUpdateDateTime >= zonedDateTime)
        val variants = episodeVariantService.findAll()
        assertEquals(3, variants.size)
    }

    @Test
    fun `run old Berserk Crunchyroll`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Berserk : L'Âge d'or - Memorial Edition",
                slug = "berserk",
                image = "test.jpg",
                banner = "test.jpg",
            )
        )
        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 4,
                image = "test.jpg"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "FR-CRUN-GJWU23V97-JA-JP",
                url = "https://www.crunchyroll.com/fr/watch/GJWU23V97/prepared-for-death"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "fr-FR",
                identifier = "FR-CRUN-G50UZQEW0-FR-FR",
                url = "https://www.crunchyroll.com/fr/watch/G50UZQEW0/prepared-for-death"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(3, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(8, variants.size)
    }

    @Test
    fun `run old Berserk Crunchyroll with no updated platform variant`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Berserk : L'Âge d'or - Memorial Edition",
                slug = "berserk",
                image = "test.jpg",
                banner = "test.jpg",
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 4,
                image = "test.jpg"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "FR-CRUN-GJWU23V97-JA-JP",
                url = "https://www.crunchyroll.com/fr/watch/GJWU23V97/prepared-for-death"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "fr-FR",
                identifier = "FR-CRUN-G50UZQEW0-FR-FR",
                url = "https://www.crunchyroll.com/fr/watch/G50UZQEW0/prepared-for-death"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(3, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(8, variants.size)
    }

    @Test
    fun `run old ADN episodes Isekai Cheat Magician`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Isekai Cheat Magician",
                slug = "isekai-cheat-magician",
                image = "https://image.animationdigitalnetwork.fr/license/cheatmagician/tv/web/affiche_350x500.jpg",
                banner = "https://image.animationdigitalnetwork.fr/license/cheatmagician/tv/web/license_640x360.jpg"
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 13,
                image = "https://image.animationdigitalnetwork.fr/license/cheatmagician/tv/web/eps13_640x360.jpg",
                title = "La Nuit aux étoiles",
                description = "Taichi, Rin, Remia et Myûra sont de retour à Azpire pour participer à un festival en mémoire des morts."
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-10114-JA-JP",
                url = "https://animationdigitalnetwork.fr/video/isekai-cheat-magician/10114-episode-13-la-nuit-aux-etoiles"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(2, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(2, variants.size)
    }

    @Test
    fun `run old CRUN episodes Let's Make a Mug Too`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Let's Make a Mug Too",
                slug = "lets-make-a-mug-too",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/4baa218403d5aa914114eb001e406f9a.jpe",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/509536f6e1d4757907762591182fd845.jpe"
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 9,
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/820b8839ea3260a5ca82a9ecaf25de96.jpe",
                title = "Écraser, étirer, reculer, compléter",
                description = "Himeno n'a toujours pas trouvé l'idée ultime pour le concours. Elle essaie donc plusieurs formes pour créer le coussin qu'elle désire."
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "FR-CRUN-GK9U383WX-JA-JP",
                url = "https://www.crunchyroll.com/fr/watch/GK9U383WX/pound-stretch-subtract-and-add"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(1, variants.size)
    }

    @Test
    fun `run old ADN episodes One Piece`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "One Piece",
                slug = "one-piece",
                image = "https://image.animationdigitalnetwork.fr/license/cheatmagician/tv/web/affiche_350x500.jpg",
                banner = "https://image.animationdigitalnetwork.fr/license/cheatmagician/tv/web/license_640x360.jpg"
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 816,
                image = "https://image.animationdigitalnetwork.fr/license/cheatmagician/tv/web/eps13_640x360.jpg",
                title = "La Nuit aux étoiles",
                description = "Taichi, Rin, Remia et Myûra sont de retour à Azpire pour participer à un festival en mémoire des morts."
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-16948-JA-JP",
                url = "https://animationdigitalnetwork.fr/video/isekai-cheat-magician/10114-episode-13-la-nuit-aux-etoiles"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(3, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(6, variants.size)
    }

    @Test
    fun `run old ADN episodes One Piece only french dub`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "One Piece",
                slug = "one-piece",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/757bae5a21039bac6ebace5de9affcd8.jpg",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/a249096c7812deb8c3c2c907173f3774.jpg"
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 566,
                image = "https://image.animationdigitalnetwork.fr/license/onepiece/tv/web/eps566_640x360.jpg",
                title = "Conclusion. L'affrontement final contre Hody !",
                description = null
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.ANIM,
                audioLocale = "fr-FR",
                identifier = "FR-ANIM-13530-FR-FR",
                url = "https://animationdigitalnetwork.fr/video/one-piece-saga-8-ile-des-hommes-poissons/13530-episode-566-conclusion-l-affrontement-final-contre-hody"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(3, mappings.size)

        episodeVariantService.findAllByMapping(episodeMapping).let { variants ->
            assertTrue(variants.map { it.audioLocale }.contains("ja-JP"))
            assertEquals(2, variants.size)
        }

        val findPreviousEpisode = episodeMappingService.findAllByAnime(anime).find { it.number == 565 }
        assertNotNull(findPreviousEpisode)

        episodeVariantService.findAllByMapping(findPreviousEpisode!!).let { variants ->
            assertTrue(variants.map { it.audioLocale }.contains("fr-FR"))
            assertEquals(2, variants.size)
        }

        updateEpisodeMappingJob.run()

        episodeVariantService.findAllByMapping(episodeMapping).let { variants ->
            assertTrue(variants.map { it.audioLocale }.contains("ja-JP"))
            assertEquals(2, variants.size)
        }

        episodeVariantService.findAllByMapping(findPreviousEpisode).let { variants ->
            assertTrue(variants.map { it.audioLocale }.contains("fr-FR"))
            assertEquals(2, variants.size)
        }
    }

    @Test
    fun `run update with multiples differents platforms`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "DAN DA DAN",
                slug = "dan-da-dan",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/13839ea2b48b0323417b23813a090c93.jpg",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/fa62dd1fc7a9bc0b587f36f53bf572c1.jpg",
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 1,
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/39d31aee335444ba382668b17b85c429.jpg"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.NETF,
                audioLocale = "ja-JP",
                identifier = "FR-NETF-a7b9feca-JA-JP",
                url = "https://www.netflix.com/fr/title/81736884"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.ANIM,
                audioLocale = "fr-FR",
                identifier = "FR-ANIM-26662-FR-FR",
                url = "https://animationdigitalnetwork.fr/video/dan-da-dan/26662-episode-1-serait-ce-une-romance-qui-commence"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "FR-ANIM-26662-JA-JP",
                url = "https://animationdigitalnetwork.fr/video/dan-da-dan/26662-episode-1-serait-ce-une-romance-qui-commence"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "FR-CRUN-GN7UNXWMJ-JA-JP",
                url = "https://www.crunchyroll.com/fr/watch/GN7UNXWMJ/thats-how-love-starts-ya-know"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.NETF,
                audioLocale = "fr-FR",
                identifier = "FR-NETF-a7b9feca-FR-FR",
                url = "https://www.netflix.com/fr/title/81736884"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "fr-FR",
                identifier = "FR-CRUN-GG1UXWE24-FR-FR",
                url = "https://www.crunchyroll.com/fr/watch/GG1UXWE24/"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(2, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(10, variants.size)
    }

    @Test
    fun `run update with bad netflix url`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Garôden : La voie du loup solitaire",
                slug = "garoden",
                image = "https://resizing.flixster.com/XksHj1sbnxReEwu8UxxkTQPgsaM=/fit-in/705x460/v2/https://resizing.flixster.com/-XZAfHZM39UwaGJIFWKAE8fS0ak=/v3/t/assets/p27251158_b_v9_ac.jpg",
                banner = "https://occ-0-56-55.1.nflxso.net/dnm/api/v6/6AYY37jfdO6hpXcMjf9Yu5cnmO0/AAAABW4lOpbtZJEWTEWnn5Pg9mXu5wD-zf4SwbqsPGuk_pckstx2SsWMcNRxGC_kI7xv6CgGNKj9MiI8RBUFojGDKmvJNnqw0X4vVFmJ.jpg",
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 7,
                image = "https://occ-0-56-55.1.nflxso.net/dnm/api/v6/9pS1daC2n6UGc3dUogvWIPMR_OU/AAAABeDPj8bjJgkLhwSHPXEEJggZGtaq_iSBmLsNP4DcgMrzUEuUVFe9rVuJROQpB4DNuPk5vyC4qd0VnmOu3YhOm4L2tw2ScA6a8T7VCxjCniE77CXmSGemQzno.jpg"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.NETF,
                audioLocale = "ja-JP",
                identifier = "FR-NETF-6c247a4d-JA-JP",
                url = "https://www.netflix.com/fr/title/6c247a4d"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(1, variants.size)
    }

    @Test
    fun `run update with netflix platform`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Du mouvement de la Terre",
                slug = "du-mouvement-de-la-terre",
                image = "https://cdn.myanimelist.net/images/anime/1713/144437l.jpg",
                banner = "https://occ-0-56-55.1.nflxso.net/dnm/api/v6/6AYY37jfdO6hpXcMjf9Yu5cnmO0/AAAABTkMGRnW-6lH_YEXhGdcFKZM-rO0bZCKzA8846_wW3Xy_s1NN72RFbddfuh9-2L_nr5lvAlFxB6i7uvUEY49CTFLoAJZA2Ag_hY6.jpg",
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 9,
                image = "https://occ-0-56-55.1.nflxso.net/dnm/api/v6/9pS1daC2n6UGc3dUogvWIPMR_OU/AAAABT_ClBwV1ItifEAGonpxHrB_b1fSWtllElp6Sl3awusb-bRXPFrTzSZaunQ4O6ku1o8CVQjKu9bEnlOYYwPGBFQZFJUg3Y8eiVroGRTd9HAOJ_S-42Yut-g-.jpg"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.NETF,
                audioLocale = "ja-JP",
                identifier = "FR-NETF-6936fd55-JA-JP",
                url = "https://www.netflix.com/fr/title/81765022"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        assumeFalse("https://occ-0-56-55.1.nflxso.net/dnm/api/v6/9pS1daC2n6UGc3dUogvWIPMR_OU/AAAABT_ClBwV1ItifEAGonpxHrB_b1fSWtllElp6Sl3awusb-bRXPFrTzSZaunQ4O6ku1o8CVQjKu9bEnlOYYwPGBFQZFJUg3Y8eiVroGRTd9HAOJ_S-42Yut-g-.jpg" == mappings.first().image)
        val variants = episodeVariantService.findAll()
        assertEquals(1, variants.size)
    }

    @Test
    fun `run update with prime video platform`() {
        val zonedDateTime = ZonedDateTime.now().minusMonths(2)

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Ninja Kamui",
                slug = "ninja-kamui",
                image = "https://cdn.myanimelist.net/images/anime/1142/141351.jpg",
                banner = "https://m.media-amazon.com/images/S/pv-target-images/3c8f2a486dc00aab40c25ffc6cf4445f2e9ea295682fade1ce466a5c22e4feba._SX1080_FMjpg_.jpg",
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 10,
                image = "https://m.media-amazon.com/images/S/pv-target-images/e2a2062260d7f35cc714079ae69ca846ca9e28364f9f1fcd92411e8d60b36315._AC_SX1920_FMjpg_.jpg"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.PRIM,
                audioLocale = "ja-JP",
                identifier = "FR-PRIM-0bdc4c77-JA-JP",
                url = "https://www.primevideo.com/-/fr/detail/0QN9ZXJ935YBTNK8U9FV5OAX5B"
            )
        )

        MapCache.invalidateAll()

        updateEpisodeMappingJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(1, variants.size)
    }
}