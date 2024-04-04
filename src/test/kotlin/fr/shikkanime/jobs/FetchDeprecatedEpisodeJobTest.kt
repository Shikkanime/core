package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.animes.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.*
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.ConfigService
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.utils.Constant
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

        val listFiles = ClassLoader.getSystemClassLoader().getResource("animes")?.file?.let { File(it).listFiles() }

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