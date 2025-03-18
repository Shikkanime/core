package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.wrappers.factories.AbstractNetflixWrapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class UpdateAnimeJobTest : AbstractTest() {
    @Inject
    private lateinit var updateAnimeJob: UpdateAnimeJob

    @BeforeEach
    override fun setUp() {
        super.setUp()
        AbstractNetflixWrapper.checkLanguage = false
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        AbstractNetflixWrapper.checkLanguage = true
    }

    @Test
    fun `update Crunchyroll anime Date A Live`() {
        val zonedDateTime = ZonedDateTime.parse("2015-08-22T01:00:00Z")

        val tmpAnime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                name = "Date A Live",
                slug = "date-a-live",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/20fc1f9c50e855bb8bbeefeab10434ff.jpe",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/3831353540b0f1547c202f2df446cf2c.jpe",
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.CRUN,
                platformId = "GYEX5E1G6"
            )
        )

        updateAnimeJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val anime = animes.first()

        assertEquals(
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/bf076cb2bfaeb7c394185c8c916f1ef5.jpg",
            anime.image
        )
        assertEquals(
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/60db2c4acbf891c7ea5f2d7982c43163.jpg",
            anime.banner
        )
        assertEquals(
            "Un tremblement spatial massif dévaste le centre du continent eurasien, ôtant la vie à plus de 150 millions de personnes. 30 ans plus tard, un nouveau tremblement spatial se produit, sous les yeux de Shidô Itsuka, un simple lycéen pas très doué en histoires de cœur. Il fait alors la rencontre d’une jeune fille qui s’avère être un esprit. Elle est un désastre qui menace l'humanité, un monstre d'origine inconnue, une existence rejetée par le monde. Pour s'en défaire, deux solutions : recourir à la force ou à la séduction. Shidô choisira la deuxième option. Entrez dans une nouvelle ère de drague !",
            anime.description
        )
    }

    @Test
    fun `update ADN and Crunchyroll anime My Deer Friend Nokotan`() {
        val zonedDateTime = ZonedDateTime.parse("2024-07-07T15:00:00Z")

        val tmpAnime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                name = "My Deer Friend Nokotan",
                slug = "my-deer-friend-nokotan",
                image = "https://image.animationdigitalnetwork.fr/license/mydeerfriendnokotan/tv/web/affiche_350x500.jpg",
                banner = "https://image.animationdigitalnetwork.fr/license/mydeerfriendnokotan/tv/web/license_640x360.jpg",
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.ANIM,
                platformId = "1188"
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.CRUN,
                platformId = "G0XHWM100"
            )
        )

        updateAnimeJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val anime = animes.first()

        assertEquals(
            "https://image.animationdigitalnetwork.com/license/mydeerfriendnokotan/tv/web/affiche_1560x2340.jpg",
            anime.image
        )
        assertEquals(
            "https://image.animationdigitalnetwork.com/license/mydeerfriendnokotan/tv/web/license_1920x1080.jpg",
            anime.banner
        )
        assertEquals(
            "Adulée par ses camarades pour son assiduité et sa beauté, Torako cache pour autant un lourd passé de délinquante. Mais lorsque la pétillante Nokotan est transférée dans sa classe, c’est la panique. Avec son nez capable de flairer les reliquats de sa vie antérieure, le chaos s’immisce peu à peu dans son quotidien. Qui est cette mystérieuse jeune fille aux bois de cerf ? Une comédie déjantée à découvrir dès maintenant !",
            anime.description
        )
    }

    @Test
    fun `update Crunchyroll anime My Hero Academia`() {
        val zonedDateTime = ZonedDateTime.parse("2015-08-22T01:00:00Z")

        val tmpAnime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                name = "My Hero Academia",
                slug = "my-hero-academia",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/6e008ad5211c3998b8f3e4bc166821cd.jpg",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/9ca680632ac63f44c7220f61ace9a81b.jpg",
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.CRUN,
                platformId = "GMEHME5WK"
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.CRUN,
                platformId = "G6NQ5DWZ6"
            )
        )

        updateAnimeJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val anime = animes.first()

        assertEquals("https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/6e008ad5211c3998b8f3e4bc166821cd.jpg", anime.image)
        assertEquals("https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/9ca680632ac63f44c7220f61ace9a81b.jpg", anime.banner)
        assertEquals("Super héros, super pouvoirs… On a tous déjà rêvé secrètement de posséder une qualité hors du commun, de briller ou d’être LA personne la plus puissante de l’univers. Dans ce nouveau monde, ce rêve est à la portée de quasiment toute la population car les humains peuvent désormais naître avec un pouvoir : le « alter ». Mais certains malchanceux naissent sans alter.", anime.description)
    }

    @Test
    fun `update Crunchyroll anime KONOSUBA`() {
        val tmpAnime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = ZonedDateTime.parse("2023-02-23T19:30:00Z"),
                lastReleaseDateTime = ZonedDateTime.parse("2024-06-19T15:00:00Z"),
                lastUpdateDateTime = ZonedDateTime.parse("2000-01-01T00:00:00Z"),
                name = "KONOSUBA -God's blessing on this wonderful world!",
                slug = "KONOSUBA",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/73224ab37d0b82b01a6748b690569511.jpg",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/26f4a1d5cd1369fb15c7d52a7d7a3105.jpg",
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.CRUN,
                platformId = "GYE5K3GQR"
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.CRUN,
                platformId = "GJ0H7Q5V7"
            )
        )

        updateAnimeJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val anime = animes.first()

        assumeTrue("https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/041fa9860f09efb08b3c8a3af712b985.jpg" == anime.image)
        assertEquals("https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/131f32cf27743b9c95b78b4b3fb1c6ee.jpg", anime.banner)
        assertEquals("Après avoir perdu la vie dans un accident de la route, Kazuma Satô voit apparaître devant lui une ravissante déesse, Aqua. Elle lui propose d’être réincarné dans un autre monde et d’emporter une seule chose avec lui. Kazuma choisit… la déesse ! Les voici transportés dans un pays sous la coupe d’un terrible roi démon. C’est le début de nombreuses aventures, car si Kazuma n’aspire qu’à la paix et au calme, Aqua se préoccupe beaucoup des problèmes des gens qu’ils croisent. Et cela ne plaît pas du tout au roi…", anime.description)
    }

    @Test
    fun `update ADN and Crunchyroll anime ONE PIECE Log Fish-Man Island Saga`() {
        val zonedDateTime = ZonedDateTime.parse("2024-11-03T08:00:00Z")

        val tmpAnime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                name = "ONE PIECE Log: Fish-Man Island Saga",
                slug = "one-piece-log",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/757bae5a21039bac6ebace5de9affcd8.jpg",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/a249096c7812deb8c3c2c907173f3774.jpg",
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.ANIM,
                platformId = "1260"
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.CRUN,
                platformId = "GRMG8ZQZR"
            )
        )

        updateAnimeJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val anime = animes.first()

        assertEquals("https://image.animationdigitalnetwork.com/license/onepiecefishmansaga/tv/web/affiche_1560x2340.jpg", anime.image)
        assertEquals("https://image.animationdigitalnetwork.com/license/onepiecefishmansaga/tv/web/license_1920x1080.jpg", anime.banner)
        assertEquals("Après deux ans de séparation, le grand jour est enfin arrivé : l’équipage du Chapeau de paille se réunit à nouveau sur l’archipel Sabaody. Forts de leur expérience et prêts à affronter le Nouveau Monde, ils se préparent à naviguer sous l’eau, avec un Thousand Sunny désormais équipé pour atteindre leur prochaine destination : l’Île des Hommes-Poissons, un paradis sous-marin à plus de 10 000 mètres de profondeur !", anime.description)
    }

    @Test
    fun `update DAN DA DAN on multiple platforms`() {
        val zonedDateTime = ZonedDateTime.parse("2024-11-03T08:00:00Z")

        val tmpAnime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                name = "DAN DA DAN",
                slug = "dan-da-dan",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/13839ea2b48b0323417b23813a090c93.jpg",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/fa62dd1fc7a9bc0b587f36f53bf572c1.jpg",
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.ANIM,
                platformId = "1160"
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.CRUN,
                platformId = "GG5H5XQ0D"
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.NETF,
                platformId = "81736884"
            )
        )

        updateAnimeJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val anime = animes.first()

        assertEquals("https://image.animationdigitalnetwork.com/license/dandadan/tv/web/affiche_1560x2340.jpg", anime.image)
        assertEquals("https://image.animationdigitalnetwork.com/license/dandadan/tv/web/license_1920x1080.jpg", anime.banner)
        assertEquals("Si Momo Ayase est persuadée de l’existence des fantômes, Okarun, quant à lui, croit dur comme fer à la présence d’extraterrestres. Pour démontrer la véracité de leurs propos, ils se lancent un pari fou : explorer des lieux chargés d’énergie occulte, sans se douter un seul instant qu’ils sont sur le point de vivre une aventure des plus singulières…", anime.description)
    }

    @Test
    fun `update Prime Video anime Ninja Kamui`() {
        val zonedDateTime = ZonedDateTime.parse("2024-02-11T15:00:00Z")

        val tmpAnime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                name = "Ninja Kamui",
                slug = "ninja-kamui",
                image = "https://cdn.myanimelist.net/images/anime/1142/141351.jpg",
                banner = "https://m.media-amazon.com/images/S/pv-target-images/3c8f2a486dc00aab40c25ffc6cf4445f2e9ea295682fade1ce466a5c22e4feba._SX1080_FMjpg_.jpg",
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.PRIM,
                platformId = "0QN9ZXJ935YBTNK8U9FV5OAX5B"
            )
        )

        updateAnimeJob.run()
    }

    @Test
    fun `update ADN and Crunchyroll anime One Piece Fan Letter`() {
        val zonedDateTime = ZonedDateTime.parse("2024-10-20T07:00:00Z")

        val tmpAnime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                name = "ONE PIECE FAN LETTER",
                slug = "one-piece-fan-letter",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/757bae5a21039bac6ebace5de9affcd8.jpg",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/a249096c7812deb8c3c2c907173f3774.jpg",
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.ANIM,
                platformId = "1258"
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = tmpAnime,
                platform = Platform.CRUN,
                platformId = "GRMG8ZQZR"
            )
        )

        updateAnimeJob.run()

        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val anime = animes.first()

        assertEquals("https://image.animationdigitalnetwork.com/license/onepiecefanletter/tv/web/affiche_1560x2340.jpg", anime.image)
        assertEquals("https://image.animationdigitalnetwork.com/license/onepiecefanletter/tv/web/license_1920x1080.jpg", anime.banner)
        assertEquals("L’intrigue se déroule sur l’île de Sabaody, deux ans après la bataille de Marine Ford, où Luffy a tragiquement perdu son frère, Ace. Une jeune fille, fascinée par Nami, entreprend alors une aventure hors du commun.", anime.description)
    }
}