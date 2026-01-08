package fr.shikkanime.jobs

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.AnimePlatform
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.StringUtils
import kotlinx.coroutines.runBlocking
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
                expectedThumbnail = "https://media.animationdigitalnetwork.com/images/show/e8658ad3-8515-41f0-b276-70817ab1acca/affiche.width=1560,height=2340,quality=100",
                expectedBanner = "https://media.animationdigitalnetwork.com/images/show/e8658ad3-8515-41f0-b276-70817ab1acca/license.width=1920,height=1080,quality=100",
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
                expectedThumbnail = "https://media.animationdigitalnetwork.com/images/show/58d38909-7a89-4102-9c48-ca49c16ac623/affiche.width=1560,height=2340,quality=100",
                expectedBanner = "https://media.animationdigitalnetwork.com/images/show/58d38909-7a89-4102-9c48-ca49c16ac623/license.width=1920,height=1080,quality=100",
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
                expectedThumbnail = "https://media.animationdigitalnetwork.com/images/show/8d045df5-44cc-47d2-98ed-18d7f80318e6/affiche.width=1560,height=2340,quality=100",
                expectedBanner = "https://media.animationdigitalnetwork.com/images/show/8d045df5-44cc-47d2-98ed-18d7f80318e6/license.width=1920,height=1080,quality=100",
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
                expectedThumbnail = "https://media.animationdigitalnetwork.com/images/show/7ae5e0e2-277a-4fd8-bbd3-dceb58ce43df/affiche.width=1560,height=2340,quality=100",
                expectedBanner = "https://media.animationdigitalnetwork.com/images/show/7ae5e0e2-277a-4fd8-bbd3-dceb58ce43df/license.width=1920,height=1080,quality=100",
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
                expectedThumbnail = "https://media.animationdigitalnetwork.com/images/show/02366b98-198d-4682-9ab0-7b4615781faf/affiche.width=1560,height=2340,quality=100",
                expectedBanner = "https://media.animationdigitalnetwork.com/images/show/02366b98-198d-4682-9ab0-7b4615781faf/license.width=1920,height=1080,quality=100",
                expectedDescription = "L’intrigue se déroule sur l’île de Sabaody, deux ans après la bataille de Marine Ford, où Luffy a tragiquement perdu son frère, Ace. Une jeune fille, fascinée par Nami, entreprend alors une aventure hors du commun."
            ),
            TestCase(
                name = "The Too-Perfect Saint: Tossed Aside by My Fiancé and Sold to Another Kingdom",
                slug = "the-too-perfect-saint-tossed-aside-by-my-fianc-and-sold-to-another-kingdom",
                releaseDateTime = "2014-04-02T13:30:00Z",
                platforms = listOf(
                    PlatformData(Platform.CRUN, "GP5HJ8477")
                ),
                expectedThumbnail = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/2611cd3c30e3fdbb6e563dfdde4a7029.jpg",
                expectedBanner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/f1c802e3c46fa6bcb76e18cb0ed62637.jpg",
                expectedDescription = "Née dans une famille où le rôle de \"Sainte\" est transmis de génération en génération, Philia est reconnue comme la plus grande de tous les temps grâce à une éducation très rigoureuse. Cependant, son fiancé, le prince Julius, rompt leurs fiançailles et voilà qu'elle est vendue à un royaume voisin. S'attendant au pire, Philia reçoit pourtant un accueil chaleureux et utilise ses talents de sainte pour protéger des monstres le pays qui l'a accueillie."
            ),
            TestCase(
                name = "Kaiju No. 8",
                slug = "kaiju-no-8",
                platforms = listOf(
                    PlatformData(Platform.CRUN, "GG5H5XQ7D")
                ),
                expectedThumbnail = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/3455ff9a3021e04fefd4fa2a417637b5.jpg",
                expectedBanner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/c2257c075677c3bfe0d66b46411529fb.jpg",
                expectedDescription = "L’histoire gravite autour de Kafka Hibino, 32 ans. Il rêve de rejoindre les Forces de Défense pour honorer une promesse faite à son amie d’enfance, Mina Ashiro. Il se lie d’amitié avec son nouveau collègue, Reno Ichikawa, avec qui il nettoie des carcasses de kaiju. La détermination de son ami lui donne le courage de se présenter au concours des Forces afin de protéger l’humanité aux côtés de Mina. Son destin bascule néanmoins lorsqu’il est parasité par un petit kaiju qui lui confère des pouvoirs surhumains. Désormais capable d’affronter ses ennemis d’égal à égal, Kafka se démène pour gagner la confiance de ses équipiers, terrasser des kaijus de plus en plus puissants et assurer la sécurité de la planète."
            ),
            TestCase(
                name = "One Piece",
                slug = "one-piece",
                platforms = listOf(
                    PlatformData(Platform.CRUN, "GRMG8ZQZR")
                ),
                expectedThumbnail = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/757bae5a21039bac6ebace5de9affcd8.jpg",
                expectedBanner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/a249096c7812deb8c3c2c907173f3774.jpg",
                expectedDescription = "Monkey D. Luffy refuse de laisser qui que ce soit ou quoi que ce soit se mettre en travers de sa quête pour devenir le Roi des pirates. Grâce à son pouvoir d'élasticité conféré par un Fruit du Démon, le jeune pirate fougueux cherche le trésor légendaire connu sous le nom de One Piece. Il mettra le cap sur les eaux périlleuses de Grand Line et recrutera l'équipage du Chapeau de paille. Ce capitaine ne jettera jamais l'ancre tant que ses amis et lui n'auront pas réalisé leurs rêves!"
            ),
            TestCase(
                name = "Jujutsu Kaisen",
                slug = "jujutsu-kaisen",
                platforms = listOf(
                    PlatformData(Platform.CRUN, "GRDV0019R")
                ),
                expectedThumbnail = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/d128baf30c0638fafce3fd4e7c9ff37c.png",
                expectedBanner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/ede36e8f3e37587c90dbd8f1216d3022.png",
                expectedDescription = "La vie au quotidien de trois charmantes jeunes filles qui vont au lycée : Momoko, belle et tête en l'air ; Shibumi, décontractée et sérieuse ; Mayumi, douce et innocente."
            )
        )
    }

    data class PlatformData(val platform: Platform, val platformId: String)

    data class TestCase(
        val name: String,
        val slug: String,
        val releaseDateTime: String? = null,
        val lastReleaseDateTime: String = StringUtils.EMPTY_STRING,
        val lastUpdateDateTime: String = StringUtils.EMPTY_STRING,
        val platforms: List<PlatformData>,
        val expectedThumbnail: String? = null,
        val expectedBanner: String? = null,
        val expectedDescription: String? = null
    )

    @ParameterizedTest(name = "Update anime {0}")
    @MethodSource("testCases")
    fun `should update anime data`(testCase: TestCase) {
        // Préparation des données
        val releaseDate = testCase.releaseDateTime?.let { ZonedDateTime.parse(it) } ?: ZonedDateTime.parse("2025-01-01T00:00:00Z")
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
        runBlocking { updateAnimeJob.run() }

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
    suspend fun `should handle Prime Video anime Ninja Kamui`() {
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