package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import io.mockk.every
import io.mockk.mockkClass
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class EpisodeVariantServiceTest {
    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var configService: ConfigService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var animePlatformService: AnimePlatformService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
        configService.save(Config(propertyKey = ConfigPropertyKey.SIMULCAST_RANGE.key, propertyValue = "15"))
        MapCache.invalidate(Config::class.java, Anime::class.java)
    }

    @AfterEach
    fun tearDown() {
        episodeVariantService.deleteAll()
        episodeMappingService.deleteAll()
        configService.deleteAll()
        animePlatformService.deleteAll()
        animeService.deleteAll()
        MapCache.invalidate(Config::class.java, Anime::class.java)
    }

    @Test
    fun `getSimulcasts get next`() {
        val anime = Anime(countryCode = CountryCode.FR, name = "Test Anime", image = "test.jpg", banner = "test.jpg", slug = "test-anime")
        val episode = mockkClass(EpisodeMapping::class)

        anime.releaseDateTime = ZonedDateTime.parse("2023-12-20T16:00:00Z")
        every { episode.releaseDateTime } returns anime.releaseDateTime
        every { episode.episodeType } returns EpisodeType.EPISODE
        every { episode.number } returns 1

        animeService.save(anime)
        val simulcast = episodeVariantService.getSimulcast(anime, episode)

        assertEquals("WINTER", simulcast.season)
        assertEquals(2024, simulcast.year)
    }

    @Test
    fun `getSimulcasts continue on current`() {
        val anime = Anime(countryCode = CountryCode.FR, name = "Test Anime", image = "test.jpg", banner = "test.jpg", slug = "test-anime")
        val episode = mockkClass(EpisodeMapping::class)
        val finalRelease = ZonedDateTime.parse("2024-01-03T16:00:00Z")

        anime.releaseDateTime = finalRelease.minusWeeks(12)
        anime.simulcasts.add(Simulcast(season = "AUTUMN", year = 2023))
        animeService.save(anime)

        (1..<12).map { i ->
            episodeMappingService.save(
                EpisodeMapping(
                    anime = anime,
                    episodeType = EpisodeType.EPISODE,
                    season = 1,
                    number = i,
                    releaseDateTime = anime.releaseDateTime.plusWeeks(i.toLong()),
                    image = "test.jpg",
                )
            ).apply {
                episodeVariantService.save(
                    EpisodeVariant(
                        mapping = this,
                        platform = Platform.CRUN,
                        audioLocale = "ja-JP",
                        identifier = "test-episode-$i",
                        url = "https://test.com/episode-$i",
                    )
                )
            }
        }

        every { episode.releaseDateTime } returns finalRelease
        every { episode.episodeType } returns EpisodeType.EPISODE
        every { episode.number } returns 12

        val simulcast = episodeVariantService.getSimulcast(anime, episode)

        assertEquals("AUTUMN", simulcast.season)
        assertEquals(2023, simulcast.year)
    }

    @Test
    fun `save platform episode`() {
        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "GNVHKN7M4",
                "Shikimori n’est pas juste mignonne",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/57da95e93614672250ff0312b4c8194c.jpe",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/c1aa33105d2acdcf7807310743b01948.jpe",
                "Izumi est un lycéen maladroit et malchanceux. Pourtant, c’est ce qui fait son charme et lui a permis de sortir avec Shikimori. Cette camarade de classe est très jolie, elle a un beau sourire et semble toujours heureuse en compagnie d’Izumi. Pourtant, le garçon ne peut s’empêcher de complexer ! Il fait tout pour continuer de la séduire, même si ses actions ne l’aident pas vraiment dans sa tâche…",
                ZonedDateTime.parse("2021-05-21T18:15:00Z"),
                EpisodeType.SPECIAL,
                1,
                -1,
                1404,
                "Commentaire audio",
                "Les interprètes de Shikimori, d’Izumi et de Hachimitsu commentent le premier épisode.",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/4dfb75a0af21c5ca84014d47f67ad176.jpe",
                Platform.CRUN,
                "ja-JP",
                "GVWU0Q0J9",
                "https://www.crunchyroll.com/fr/watch/GVWU0Q0J9/special",
                uncensored = false,
                original = true,
            ),
            updateMappingDateTime = false
        )

        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val mapping = mappings.first()
        assertEquals(EpisodeType.SPECIAL, mapping.episodeType)
        assertEquals(1, mapping.season)
        assertEquals(1, mapping.number)
    }

    @Test
    fun `save dan da dan multiple platform`() {
        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "1160",
                "DAN DA DAN",
                "https://image.animationdigitalnetwork.fr/license/dandadan/tv/web/affiche_350x500.jpg",
                "https://image.animationdigitalnetwork.fr/license/dandadan/tv/web/license_640x360.jpg",
                "Si Momo Ayase est persuadée de l’existence des fantômes, Okarun, quant à lui, croit dur comme fer à la présence d’extraterrestres. Pour démontrer la véracité de leurs propos, ils se lancent un pari fou : explorer des lieux chargés d’énergie occulte, sans se douter un seul instant qu’ils sont sur le point de vivre une aventure des plus singulières…",
                ZonedDateTime.parse("2024-10-03T16:00:00Z"),
                EpisodeType.EPISODE,
                1,
                1,
                1442,
                "Serait-ce une romance qui commence ?",
                "Okarun ne croit pas aux fantômes, mais il croit aux aliens. Pour Momo, c’est l’inverse, elle croit aux fantômes, mais pas aux aliens. Les deux se lancent un défi afin de savoir qui a raison. Mais en explorant des lieux abandonnés, leur petit défi va prendre des proportions qui dépassent l’entendement.",
                "https://image.animationdigitalnetwork.fr/license/dandadan/tv/web/eps1_640x360.jpg",
                Platform.ANIM,
                "ja-JP",
                "26662",
                "https://animationdigitalnetwork.fr/video/dan-da-dan/26662-episode-1-serait-ce-une-romance-qui-commence",
                uncensored = false,
                original = true,
            )
        )

        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "1160",
                "DAN DA DAN",
                "https://image.animationdigitalnetwork.fr/license/dandadan/tv/web/affiche_350x500.jpg",
                "https://image.animationdigitalnetwork.fr/license/dandadan/tv/web/license_640x360.jpg",
                "Si Momo Ayase est persuadée de l’existence des fantômes, Okarun, quant à lui, croit dur comme fer à la présence d’extraterrestres. Pour démontrer la véracité de leurs propos, ils se lancent un pari fou : explorer des lieux chargés d’énergie occulte, sans se douter un seul instant qu’ils sont sur le point de vivre une aventure des plus singulières…",
                ZonedDateTime.parse("2024-10-03T16:00:00Z"),
                EpisodeType.EPISODE,
                1,
                1,
                1442,
                "Serait-ce une romance qui commence ?",
                "Okarun ne croit pas aux fantômes, mais il croit aux aliens. Pour Momo, c’est l’inverse, elle croit aux fantômes, mais pas aux aliens. Les deux se lancent un défi afin de savoir qui a raison. Mais en explorant des lieux abandonnés, leur petit défi va prendre des proportions qui dépassent l’entendement.",
                "https://image.animationdigitalnetwork.fr/license/dandadan/tv/web/eps1_640x360.jpg",
                Platform.ANIM,
                "fr-FR",
                "26662",
                "https://animationdigitalnetwork.fr/video/dan-da-dan/26662-episode-1-serait-ce-une-romance-qui-commence",
                uncensored = false,
                original = true,
            )
        )

        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "GG5H5XQ0D",
                "DAN DA DAN",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/13839ea2b48b0323417b23813a090c93.jpg",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/fa62dd1fc7a9bc0b587f36f53bf572c1.jpg",
                "Quand Momo, issue d'une lignée de médiums, rencontre son camarade de classe Okarun, fasciné par l’occulte, ils se disputent. Lorsqu'il s'avère que les deux phénomènes sont bien réels, Momo réveille en elle un pouvoir caché tandis qu'une malédiction s'abat sur Okarun. Ensemble, ils doivent affronter les forces du paranormal qui menacent notre monde...",
                ZonedDateTime.parse("2024-10-03T16:00:00Z"),
                EpisodeType.EPISODE,
                1,
                1,
                1437,
                "Serait-ce une romance qui commence ?",
                "Okarun ne croit pas aux fantômes, mais il croit aux aliens. Pour Momo, c’est l’inverse, elle croit aux fantômes, mais pas aux aliens. Les deux se lancent un défi afin de savoir qui a raison. Mais en explorant des lieux abandonnés, leur petit défi va prendre des proportions qui dépassent l’entendement.",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/39d31aee335444ba382668b17b85c429.jpg",
                Platform.CRUN,
                "ja-JP",
                "GN7UNXWMJ",
                "https://www.crunchyroll.com/fr/watch/GN7UNXWMJ/thats-how-love-starts-ya-know",
                uncensored = false,
                original = true,
            )
        )

        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "81736884",
                "DANDADAN",
                "https://fr.web.img6.acsta.net/pictures/24/03/18/10/50/1581066.jpg",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/fa62dd1fc7a9bc0b587f36f53bf572c1.jpg",
                "Entre menaces paranormales, nouveaux superpouvoirs et histoire d'amour naissante, deux lycéens se mettent au défi de prouver l'existence des fantômes ou des extraterrestres.",
                ZonedDateTime.parse("2024-10-03T16:00:00Z"),
                EpisodeType.EPISODE,
                1,
                1,
                1440,
                "Serait-ce une romance qui commence ?",
                "Momo est une lycéenne qui croit aux fantômes, mais pas aux extraterrestres. Un jour, elle se dispute avec un camarade de classe qui croit aux ovnis, mais pas aux esprits.",
                "https://occ-0-8194-56.1.nflxso.net/dnm/api/v6/9pS1daC2n6UGc3dUogvWIPMR_OU/AAAABX1dtlp9gxfhu0LxOrbUq_MT-KlN7dEm6VpsU5JXCZoGzRx0VqYgzgKJa1EWZRnmym_WWv5CWg3jKqLdoYYX88X7IwSHEyeilqmA8ujaaGGeyTaH_A_fUm8D.webp",
                Platform.CRUN,
                "ja-JP",
                "a7b9feca",
                "https://www.netflix.com/fr/title/81736884",
                uncensored = false,
                original = true,
            )
        )

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(4, variants.size)
    }
}