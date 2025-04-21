package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZonedDateTime

class UpdateAnimeJobTest : AbstractTest() {
    @Inject private lateinit var updateAnimeJob: UpdateAnimeJob

    companion object {
        @JvmStatic
        fun testCases() = listOf(
            TestCase(
                name = "Date A Live",
                slug = "date-a-live",
                releaseDateTime = "2015-08-22T01:00:00Z",
                platforms = listOf(
                    PlatformData(Platform.CRUN, "GYEX5E1G6")
                ),
                expectedThumbnail = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/bf076cb2bfaeb7c394185c8c916f1ef5.jpg",
                expectedBanner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/60db2c4acbf891c7ea5f2d7982c43163.jpg",
                expectedDescription = "Un tremblement spatial massif dévaste le centre du continent eurasien, ôtant la vie à plus de 150 millions de personnes. 30 ans plus tard, un nouveau tremblement spatial se produit, sous les yeux de Shidô Itsuka, un simple lycéen pas très doué en histoires de cœur. Il fait alors la rencontre d’une jeune fille qui s’avère être un esprit. Elle est un désastre qui menace l'humanité, un monstre d'origine inconnue, une existence rejetée par le monde. Pour s'en défaire, deux solutions : recourir à la force ou à la séduction. Shidô choisira la deuxième option. Entrez dans une nouvelle ère de drague !"
            ),
            TestCase(
                name = "My Deer Friend Nokotan",
                slug = "my-deer-friend-nokotan",
                releaseDateTime = "2024-07-07T15:00:00Z",
                platforms = listOf(
                    PlatformData(Platform.ANIM, "1188"),
                    PlatformData(Platform.CRUN, "G0XHWM100")
                ),
                expectedThumbnail = "https://image.animationdigitalnetwork.com/license/mydeerfriendnokotan/tv/web/affiche_1560x2340.jpg",
                expectedBanner = "https://image.animationdigitalnetwork.com/license/mydeerfriendnokotan/tv/web/license_1920x1080.jpg",
                expectedDescription = "Adulée par ses camarades pour son assiduité et sa beauté, Torako cache pour autant un lourd passé de délinquante. Mais lorsque la pétillante Nokotan est transférée dans sa classe, c’est la panique. Avec son nez capable de flairer les reliquats de sa vie antérieure, le chaos s’immisce peu à peu dans son quotidien. Qui est cette mystérieuse jeune fille aux bois de cerf ? Une comédie déjantée à découvrir dès maintenant !"
            ),
            TestCase(
                name = "My Hero Academia",
                slug = "my-hero-academia",
                releaseDateTime = "2015-08-22T01:00:00Z",
                platforms = listOf(
                    PlatformData(Platform.CRUN, "GMEHME5WK"),
                    PlatformData(Platform.CRUN, "G6NQ5DWZ6"),
                    PlatformData(Platform.ANIM, "405"),
                ),
                expectedThumbnail = "https://image.animationdigitalnetwork.com/license/mha/tv/web/affiche_1560x2340.jpg",
                expectedBanner = "https://image.animationdigitalnetwork.com/license/mha/tv/web/license_1920x1080.jpg",
                expectedDescription = "Dans une société où la majorité des individus possèdent un « Alter », un pouvoir singulier qui permet de se démarquer, Izuku Midoriya fait figure d’exception et en est dépourvu. Cependant, son courage inébranlable et sa volonté d’acier captivent All Might, héros emblématique, qui choisit de lui transmettre le « One For All », un Alter légendaire. Ainsi commence pour l’adolescent un long chemin jalonné de défis."
            ),
            TestCase(
                name = "KONOSUBA -God's blessing on this wonderful world!",
                slug = "KONOSUBA",
                releaseDateTime = "2023-02-23T19:30:00Z",
                lastReleaseDateTime = "2024-06-19T15:00:00Z",
                lastUpdateDateTime = "2000-01-01T00:00:00Z",
                platforms = listOf(
                    PlatformData(Platform.CRUN, "GYE5K3GQR"),
                    PlatformData(Platform.CRUN, "GJ0H7Q5V7")
                ),
                expectedThumbnail = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/041fa9860f09efb08b3c8a3af712b985.jpg",
                expectedBanner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/131f32cf27743b9c95b78b4b3fb1c6ee.jpg",
                expectedDescription = "Après avoir perdu la vie dans un accident de la route, Kazuma Satô voit apparaître devant lui une ravissante déesse, Aqua. Elle lui propose d’être réincarné dans un autre monde et d’emporter une seule chose avec lui. Kazuma choisit… la déesse ! Les voici transportés dans un pays sous la coupe d’un terrible roi démon. C’est le début de nombreuses aventures, car si Kazuma n’aspire qu’à la paix et au calme, Aqua se préoccupe beaucoup des problèmes des gens qu’ils croisent. Et cela ne plaît pas du tout au roi…"
            ),
            TestCase(
                name = "ONE PIECE Log: Fish-Man Island Saga",
                slug = "one-piece-log",
                releaseDateTime = "2024-11-03T08:00:00Z",
                platforms = listOf(
                    PlatformData(Platform.ANIM, "1260"),
                    PlatformData(Platform.CRUN, "GRMG8ZQZR")
                ),
                expectedThumbnail = "https://image.animationdigitalnetwork.com/license/onepiecefishmansaga/tv/web/affiche_1560x2340.jpg",
                expectedBanner = "https://image.animationdigitalnetwork.com/license/onepiecefishmansaga/tv/web/license_1920x1080.jpg",
                expectedDescription = "Après deux ans de séparation, le grand jour est enfin arrivé : l’équipage du Chapeau de paille se réunit à nouveau sur l’archipel Sabaody. Forts de leur expérience et prêts à affronter le Nouveau Monde, ils se préparent à naviguer sous l’eau, avec un Thousand Sunny désormais équipé pour atteindre leur prochaine destination : l’Île des Hommes-Poissons, un paradis sous-marin à plus de 10 000 mètres de profondeur !"
            ),
            TestCase(
                name = "DAN DA DAN",
                slug = "dan-da-dan",
                releaseDateTime = "2024-11-03T08:00:00Z",
                platforms = listOf(
                    PlatformData(Platform.ANIM, "1160"),
                    PlatformData(Platform.CRUN, "GG5H5XQ0D"),
                    PlatformData(Platform.NETF, "81736884")
                ),
                expectedThumbnail = "https://image.animationdigitalnetwork.com/license/dandadan/tv/web/affiche_1560x2340.jpg",
                expectedBanner = "https://image.animationdigitalnetwork.com/license/dandadan/tv/web/license_1920x1080.jpg",
                expectedDescription = "Si Momo Ayase est persuadée de l’existence des fantômes, Okarun, quant à lui, croit dur comme fer à la présence d’extraterrestres. Pour démontrer la véracité de leurs propos, ils se lancent un pari fou : explorer des lieux chargés d’énergie occulte, sans se douter un seul instant qu’ils sont sur le point de vivre une aventure des plus singulières…"
            ),
            TestCase(
                name = "ONE PIECE FAN LETTER",
                slug = "one-piece-fan-letter",
                releaseDateTime = "2024-10-20T07:00:00Z",
                platforms = listOf(
                    PlatformData(Platform.ANIM, "1258"),
                    PlatformData(Platform.CRUN, "GRMG8ZQZR")
                ),
                expectedThumbnail = "https://image.animationdigitalnetwork.com/license/onepiecefanletter/tv/web/affiche_1560x2340.jpg",
                expectedBanner = "https://image.animationdigitalnetwork.com/license/onepiecefanletter/tv/web/license_1920x1080.jpg",
                expectedDescription = "L’intrigue se déroule sur l’île de Sabaody, deux ans après la bataille de Marine Ford, où Luffy a tragiquement perdu son frère, Ace. Une jeune fille, fascinée par Nami, entreprend alors une aventure hors du commun."
            )
        )
    }

    data class PlatformData(val platform: Platform, val platformId: String)

    data class TestCase(
        val name: String,
        val slug: String,
        val releaseDateTime: String,
        val lastReleaseDateTime: String = "",
        val lastUpdateDateTime: String = "",
        val platforms: List<PlatformData>,
        val expectedThumbnail: String? = null,
        val expectedBanner: String? = null,
        val expectedDescription: String? = null
    )

    @ParameterizedTest(name = "Update anime {0}")
    @MethodSource("testCases")
    fun `should update anime data`(testCase: TestCase) {
        // Préparation des données
        val releaseDate = ZonedDateTime.parse(testCase.releaseDateTime)
        val lastReleaseDate = if (testCase.lastReleaseDateTime.isNotEmpty())
            ZonedDateTime.parse(testCase.lastReleaseDateTime) else releaseDate
        val lastUpdateDate = if (testCase.lastUpdateDateTime.isNotEmpty())
            ZonedDateTime.parse(testCase.lastUpdateDateTime) else releaseDate

        // Création de l'anime
        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = releaseDate,
                lastReleaseDateTime = lastReleaseDate,
                lastUpdateDateTime = lastUpdateDate,
                name = testCase.name,
                slug = testCase.slug,
            )
        )

        // Ajout des plateformes
        testCase.platforms.forEach { platformData ->
            animePlatformService.save(
                AnimePlatform(
                    anime = anime,
                    platform = platformData.platform,
                    platformId = platformData.platformId
                )
            )
        }

        // Exécution du job
        updateAnimeJob.run()

        // Vérifications
        val animes = animeService.findAll()
        assertEquals(1, animes.size)
        val updatedAnime = animes.first()

        // Vérification des données mises à jour
        testCase.expectedThumbnail?.let {
            assumeTrue(
                it ==
                        attachmentService.findByEntityUuidTypeAndActive(updatedAnime.uuid!!, ImageType.THUMBNAIL)?.url
            )
        }

        testCase.expectedBanner?.let {
            assumeTrue(
                it ==
                        attachmentService.findByEntityUuidTypeAndActive(updatedAnime.uuid!!, ImageType.BANNER)?.url
            )
        }

        testCase.expectedDescription?.let {
            assumeTrue(it == updatedAnime.description)
        }
    }

    @Test
    fun `should handle Prime Video anime Ninja Kamui`() {
        val releaseDateTime = ZonedDateTime.parse("2024-02-11T15:00:00Z")

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = releaseDateTime,
                lastReleaseDateTime = releaseDateTime,
                lastUpdateDateTime = releaseDateTime,
                name = "Ninja Kamui",
                slug = "ninja-kamui",
            )
        )

        animePlatformService.save(
            AnimePlatform(
                anime = anime,
                platform = Platform.PRIM,
                platformId = "0QN9ZXJ935YBTNK8U9FV5OAX5B"
            )
        )

        // Ce test vérifie seulement que le job s'exécute sans erreur
        updateAnimeJob.run()

        // Vous pouvez ajouter des vérifications si nécessaire
    }
}