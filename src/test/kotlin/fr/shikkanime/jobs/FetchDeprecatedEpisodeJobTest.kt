package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.*
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.ObjectParser
import fr.shikkanime.utils.ObjectParser.getAsString
import fr.shikkanime.wrappers.AnimationDigitalNetworkWrapper
import fr.shikkanime.wrappers.CrunchyrollWrapper
import fr.shikkanime.wrappers.PrimeVideoWrapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class FetchDeprecatedEpisodeJobTest {
    private val fetchDeprecatedEpisodeJob = FetchDeprecatedEpisodeJob()

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeService: EpisodeService

    @Inject
    private lateinit var configService: ConfigService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
        Constant.injector.injectMembers(fetchDeprecatedEpisodeJob)

        val listFiles = File(ClassLoader.getSystemClassLoader().getResource("animes")?.file).listFiles()

        listFiles
            ?.sortedBy { it.name.lowercase() }
            ?.forEach {
                val anime = animeService.save(
                    AbstractConverter.convert(
                        ObjectParser.fromJson(
                            it.readText(),
                            AnimeDto::class.java
                        ), Anime::class.java
                    )
                )

                (1..10).forEach { number ->
                    val platform = Platform.entries[number % Platform.entries.size]

                    val url = when (platform) {
                        Platform.CRUN -> "https://www.crunchyroll.com/media-918466"
                        Platform.ANIM -> "https://animationdigitalnetwork.fr/video/dog-signal/23690-episode-18-le-mangaka-et-le-shiba"
                        Platform.PRIM -> "https://www.primevideo.com/-/fr/detail/0QN9ZXJ935YBTNK8U9FV5OAX5B"
                        else -> "https://www.google.com"
                    }

                    episodeService.save(
                        Episode(
                            platform = platform,
                            anime = anime,
                            episodeType = EpisodeType.entries.random(),
                            langType = LangType.entries.random(),
                            hash = UUID.randomUUID().toString(),
                            releaseDateTime = anime.releaseDateTime,
                            season = 1,
                            number = number,
                            title = "Episode $number",
                            url = url,
                            image = "https://pbs.twimg.com/profile_banners/1726908281640091649/1700562801/1500x500",
                            duration = 1420
                        )
                    )
                }
            }

        configService.save(
            Config(
                propertyKey = ConfigPropertyKey.FETCH_OLD_EPISODE_DESCRIPTION_SIZE.key,
                propertyValue = "2"
            )
        )
    }

    @AfterEach
    fun tearDown() {
        episodeService.deleteAll()
        animeService.deleteAll()
        configService.deleteAll()
    }

    @Test
    fun normalizeUrl() {
        assertEquals(
            "GMKUXPD53",
            fetchDeprecatedEpisodeJob.getCrunchyrollEpisodeId("https://www.crunchyroll.com/fr/watch/GMKUXPD53/")
        )
        assertEquals(
            "G14U415N4",
            fetchDeprecatedEpisodeJob.getCrunchyrollEpisodeId("https://www.crunchyroll.com/fr/watch/G14U415N4/the-panicked-foolish-angel-and-demon")
        )
        assertEquals(
            "G14U415D2",
            fetchDeprecatedEpisodeJob.getCrunchyrollEpisodeId("https://www.crunchyroll.com/fr/watch/G14U415D2/natsukawa-senpai-is-super-good-looking")
        )
        assertEquals(
            "G8WUN158J",
            fetchDeprecatedEpisodeJob.getCrunchyrollEpisodeId("https://www.crunchyroll.com/fr/watch/G8WUN158J/")
        )
        assertEquals(
            "GEVUZD021",
            fetchDeprecatedEpisodeJob.getCrunchyrollEpisodeId("https://www.crunchyroll.com/fr/watch/GEVUZD021/becoming-a-three-star-chef")
        )
        assertEquals(
            "GK9U3KWN4",
            fetchDeprecatedEpisodeJob.getCrunchyrollEpisodeId("https://www.crunchyroll.com/fr/watch/GK9U3KWN4/yukis-world")
        )
    }

    @Test
    fun bug() {
        val token = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        val cms = runBlocking { CrunchyrollWrapper.getCMS(token) }

        val episode = Episode(
            platform = Platform.CRUN,
            anime = Anime(
                countryCode = CountryCode.FR,
                name = "Villainess Level 99: I May Be the Hidden Boss But I'm Not the Demon Lord",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/9cf39e672287c0b7d81d6ce6ba897b25.jpe",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/b759905ae99ec12686f372129ce96799.jpe",
                description = "Cette étudiante japonaise discrète est réincarnée dans le corps d’Eumiella Dolkness, la méchante de son otome game préféré. Aspirant toujours à une vie tranquille, elle n’est pas vraiment ravie et décide d’abandonner ses fonctions maléfiques. Jusqu'à ce que son côté gamer entre en jeu et qu'elle atteigne accidentellement le niveau 99 ! À présent, tout le monde la soupçonne d'être l'infâme Maître des Démons…",
                slug = "villainess-level-99"
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "FR-CRUN-918565-SUBTITLES",
            season = 1,
            number = 9,
            title = "Le boss caché se fait démarcher par un pays ennemi",
            url = "https://www.crunchyroll.com/media-918565",
            image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/f4afb9fbdd5a99bcdfbe349e6d00acb2.jpe",
            duration = 1420
        )

        fetchDeprecatedEpisodeJob.accessToken = token
        fetchDeprecatedEpisodeJob.cms = cms
        val content = fetchDeprecatedEpisodeJob.crunchyrollExternalIdToId(HttpRequest(), episode)!!
        assertEquals("G7PU418J7", content.getAsString("id"))
        assertEquals(
            "https://www.crunchyroll.com/fr/watch/G7PU418J7/the-hidden-boss-is-solicited-by-an-enemy-nation",
            fetchDeprecatedEpisodeJob.buildCrunchyrollEpisodeUrl(content, episode)
        )
    }

    @Test
    fun bug2() {
        val token = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        val cms = runBlocking { CrunchyrollWrapper.getCMS(token) }

        val episode = Episode(
            platform = Platform.CRUN,
            anime = Anime(
                countryCode = CountryCode.FR,
                name = "Bottom-Tier Character Tomozaki",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/fa8c0b715dda49a4cbb8094c4136b382.jpe",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/b2dbd10a57e485f3ba4fcab6116f7625.jpe",
                description = "Tomozaki Fumiya est à la fois l'un des meilleurs gamers du Japon et un lycéen des plus solitaires. Un jour, celui qui voit la vie comme un jeu sans intérêt rencontre l'héroïne parfaite de son établissement, Hinami Aoi... qui lui ordonne de prendre sa vie en main aussi sérieusement qu'il vit ses parties de jeu vidéo ! La vie est-elle un jeu sans intérêt ou le plus fin des plaisirs ? Avec Hinami aux manettes, une petite révolution s'amorce dans l'existence de Tomozaki !",
                slug = "bottom-tier-character-tomozaki"
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "FR-CRUN-917959-SUBTITLES",
            season = 2,
            number = 1,
            title = "Collecter des informations sans s’ennuyer, c’est parfait",
            url = "https://www.crunchyroll.com/fr/episode-1-the-best-games-make-reconnaissance-fun-917959",
            image = "https://img1.ak.crunchyroll.com/i/spire4-tmb/63cf5c6f4cbc2a0beac3c3f10b8fe3791704287894_full.jpg",
            duration = 1422
        )

        fetchDeprecatedEpisodeJob.accessToken = token
        fetchDeprecatedEpisodeJob.cms = cms
        val content = fetchDeprecatedEpisodeJob.crunchyrollExternalIdToId(HttpRequest(), episode)!!
        assertEquals("G7PU413X5", content.getAsString("id"))
        assertEquals(
            "https://www.crunchyroll.com/fr/watch/G7PU413X5/the-best-games-make-reconnaissance-fun",
            fetchDeprecatedEpisodeJob.buildCrunchyrollEpisodeUrl(content, episode)
        )
    }

    @Test
    fun normalizeDescription() {
        val content = runBlocking { AnimationDigitalNetworkWrapper.getShowVideo(24108) }
        assertEquals(
            "Alors qu'ils sont en route pour leur voyage scolaire, une classe de lycéens est invoquée dans un autre monde afin de participer à une lutte sanguinaire pour devenir des « sages », la classe dirigeante de ce nouveau monde. Chaque élève se voit attribuer un pouvoir, sauf Yogiri et Tomochika, qui ont été laissés pour mort par leurs camarades…",
            fetchDeprecatedEpisodeJob.normalizeDescription(Platform.ANIM, content)
        )
    }

    @Test
    fun normalizeImage() {
        val adnContent = runBlocking { AnimationDigitalNetworkWrapper.getShowVideo(24108) }
        assertEquals(
            "https://image.animationdigitalnetwork.fr/license/myinstantdeathability/tv/web/eps1_640x360.jpg",
            fetchDeprecatedEpisodeJob.normalizeImage(Platform.ANIM, adnContent)
        )

        val crunchyrollAccessToken = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
        val crunchyrollCMS = runBlocking { CrunchyrollWrapper.getCMS(crunchyrollAccessToken) }
        val crunchyrollContent = runBlocking {
            CrunchyrollWrapper.getObject(
                CountryCode.FR.locale,
                crunchyrollAccessToken,
                crunchyrollCMS,
                "GEVUZD0ND"
            )
        }.first()
        assertEquals(
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/94111538bdc1b3563b14ee185d99958d.jpe",
            fetchDeprecatedEpisodeJob.normalizeImage(Platform.CRUN, crunchyrollContent)
        )
    }

    @Test
    fun `normalize content for prime video`() {
        val list = runBlocking {
            PrimeVideoWrapper.getShowVideos(
                CountryCode.FR.name,
                CountryCode.FR.locale,
                "0QN9ZXJ935YBTNK8U9FV5OAX5B"
            )
        }

        val content1 = list.first { it.getAsString("id") == "FR-PRIM-467dd829-SUBTITLES" }
        assertEquals(
            "https://m.media-amazon.com/images/S/pv-target-images/3be1307dd8c3e901ca1b97c0f50142657aaa9db169ddb98c899dc8a2b1bcdaa4._AC_SX720_FMjpg_.jpg",
            fetchDeprecatedEpisodeJob.normalizeImage(Platform.PRIM, content1)
        )

        assertEquals(
            "Épisode 4",
            fetchDeprecatedEpisodeJob.normalizeTitle(Platform.PRIM, content1)
        )

        assertEquals(
            "Un mystérieux bienfaiteur prévient Higan qu'il lui est impossible d'infiltrer AUZA City sans aide. Mike et Emma n'ont que de légères blessures après leur accident et reprennent leur enquête sur AUZA et les ninjas.",
            fetchDeprecatedEpisodeJob.normalizeDescription(Platform.PRIM, content1)
        )

        val content2 = list.first { it.getAsString("id") == "FR-PRIM-de79b9d1-SUBTITLES" }

        assertEquals(
            "https://m.media-amazon.com/images/S/pv-target-images/57471c2ecc25e001050ae0a12acbfa12c3ee4da88eb7d1d142749434c4500596._AC_SX720_FMjpg_.jpg",
            fetchDeprecatedEpisodeJob.normalizeImage(Platform.PRIM, content2)
        )

        assertEquals(
            "Episode 1",
            fetchDeprecatedEpisodeJob.normalizeTitle(Platform.PRIM, content2)
        )

        assertEquals(
            "Sa famille ayant été assassinée par un groupe d'hommes masqués, Joe Logan est approché par des agents du FBI, Mike et Emma, qui enquêtent sur le meurtre de sa femme et son fils.",
            fetchDeprecatedEpisodeJob.normalizeDescription(Platform.PRIM, content2)
        )
    }

    @Test
    fun run() {
        fetchDeprecatedEpisodeJob.run()
    }
}