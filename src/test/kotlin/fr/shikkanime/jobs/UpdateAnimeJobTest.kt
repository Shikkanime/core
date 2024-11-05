package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.Platform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class UpdateAnimeJobTest : AbstractTest() {
    @Inject
    private lateinit var updateAnimeJob: UpdateAnimeJob

    @Test
    fun `run old Crunchyroll anime`() {
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
    fun `run old ADN and Crunchyroll anime`() {
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
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/6533e54a54f7a69c806920607bc8238e.jpg",
            anime.image
        )
        assertEquals(
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/9137d9a835d85f965d7a87886878c630.jpg",
            anime.banner
        )
        assertEquals(
            "Torako Koshi est une lycéenne modèle, mais personne ne sait qu’auparavant, elle était une délinquante. Un jour, elle croise la route d’une drôle de fille avec des bois sur la tête, coincée dans des lignes électriques : Shikanoko, alias Nokotan. Qui est-elle ? Un humain ? Un cerf ? En tous cas, elle connaît le secret de Torako et l’embarque dans une aventure complètement folle.",
            anime.description
        )
    }

    @Test
    fun `run old Crunchyroll anime My Hero Academia`() {
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

        assumeTrue(anime.image == "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/6e008ad5211c3998b8f3e4bc166821cd.jpg")
        assumeTrue(anime.banner == "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/9ca680632ac63f44c7220f61ace9a81b.jpg")
        assumeTrue(anime.description == "Super héros, super pouvoirs… On a tous déjà rêvé secrètement de posséder une qualité hors du commun, de briller ou d’être LA personne la plus puissante de l’univers. Dans ce nouveau monde, ce rêve est à la portée de quasiment toute la population car les humains peuvent désormais naître avec un pouvoir : le « alter ». Mais certains malchanceux naissent sans alter.")
    }
}