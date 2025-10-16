package fr.shikkanime.services

import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.*
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.utils.InvalidationService
import io.mockk.every
import io.mockk.mockkClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class EpisodeVariantServiceTest : AbstractTest() {
    @BeforeEach
    override fun setUp() {
        super.setUp()
        configService.save(Config(propertyKey = ConfigPropertyKey.SIMULCAST_RANGE.key, propertyValue = "15"))
        InvalidationService.invalidate(Config::class.java)
    }

    @Test
    fun `getSimulcasts get next`() {
        val anime = Anime(countryCode = CountryCode.FR, name = "Test Anime", slug = "test-anime")
        val episode = mockkClass(EpisodeMapping::class)

        anime.releaseDateTime = ZonedDateTime.parse("2023-12-20T16:00:00Z")
        every { episode.releaseDateTime } returns anime.releaseDateTime
        every { episode.episodeType } returns EpisodeType.EPISODE
        every { episode.number } returns 1

        animeService.save(anime)
        val simulcast = episodeVariantService.getSimulcast(anime = anime, entity = episode)

        assertEquals(Season.WINTER, simulcast.season)
        assertEquals(2024, simulcast.year)
    }

    @Test
    fun `getSimulcasts continue on current`() {
        val anime = Anime(countryCode = CountryCode.FR, name = "Test Anime", slug = "test-anime")
        val episode = mockkClass(EpisodeMapping::class)
        val finalRelease = ZonedDateTime.parse("2024-01-03T16:00:00Z")

        anime.releaseDateTime = finalRelease.minusWeeks(12)
        anime.simulcasts.add(Simulcast(season = Season.AUTUMN, year = 2023))
        animeService.save(anime)

        (1..<12).map { i ->
            episodeMappingService.save(
                EpisodeMapping(
                    anime = anime,
                    episodeType = EpisodeType.EPISODE,
                    season = 1,
                    number = i,
                    releaseDateTime = anime.releaseDateTime.plusWeeks(i.toLong()),
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

        val simulcast = episodeVariantService.getSimulcast(anime = anime, entity = episode)

        assertEquals(Season.AUTUMN, simulcast.season)
        assertEquals(2023, simulcast.year)
    }

    @Test
    fun `save platform episode`() {
        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "GNVHKN7M4",
                "Shikimori n’est pas juste mignonne",
                mapOf(
                    ImageType.THUMBNAIL to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/57da95e93614672250ff0312b4c8194c.jpe",
                    ImageType.BANNER to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/c1aa33105d2acdcf7807310743b01948.jpe",
                ),
                "Izumi est un lycéen maladroit et malchanceux. Pourtant, c’est ce qui fait son charme et lui a permis de sortir avec Shikimori. Cette camarade de classe est très jolie, elle a un beau sourire et semble toujours heureuse en compagnie d’Izumi. Pourtant, le garçon ne peut s’empêcher de complexer ! Il fait tout pour continuer de la séduire, même si ses actions ne l’aident pas vraiment dans sa tâche…",
                ZonedDateTime.parse("2021-05-21T18:15:00Z"),
                EpisodeType.SPECIAL,
                "GRDQCG2KK",
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
        ruleService.save(Rule(platform = Platform.NETF, seriesId = "81736884", seasonId = "1", action = Rule.Action.REPLACE_ANIME_NAME, actionValue = "DAN DA DAN"))
        InvalidationService.invalidate(Rule::class.java)

        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "1160",
                "DAN DA DAN",
                mapOf(
                    ImageType.THUMBNAIL to "https://image.animationdigitalnetwork.fr/license/dandadan/tv/web/affiche_350x500.jpg",
                    ImageType.BANNER to "https://image.animationdigitalnetwork.fr/license/dandadan/tv/web/license_640x360.jpg",
                ),
                "Si Momo Ayase est persuadée de l’existence des fantômes, Okarun, quant à lui, croit dur comme fer à la présence d’extraterrestres. Pour démontrer la véracité de leurs propos, ils se lancent un pari fou : explorer des lieux chargés d’énergie occulte, sans se douter un seul instant qu’ils sont sur le point de vivre une aventure des plus singulières…",
                ZonedDateTime.parse("2024-10-03T16:00:00Z"),
                EpisodeType.EPISODE,
                "1",
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
                mapOf(
                    ImageType.THUMBNAIL to "https://image.animationdigitalnetwork.fr/license/dandadan/tv/web/affiche_350x500.jpg",
                    ImageType.BANNER to "https://image.animationdigitalnetwork.fr/license/dandadan/tv/web/license_640x360.jpg",
                ),
                "Si Momo Ayase est persuadée de l’existence des fantômes, Okarun, quant à lui, croit dur comme fer à la présence d’extraterrestres. Pour démontrer la véracité de leurs propos, ils se lancent un pari fou : explorer des lieux chargés d’énergie occulte, sans se douter un seul instant qu’ils sont sur le point de vivre une aventure des plus singulières…",
                ZonedDateTime.parse("2024-10-03T16:00:00Z"),
                EpisodeType.EPISODE,
                "1",
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
                mapOf(
                    ImageType.THUMBNAIL to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/13839ea2b48b0323417b23813a090c93.jpg",
                    ImageType.BANNER to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/fa62dd1fc7a9bc0b587f36f53bf572c1.jpg",
                ),
                "Quand Momo, issue d'une lignée de médiums, rencontre son camarade de classe Okarun, fasciné par l’occulte, ils se disputent. Lorsqu'il s'avère que les deux phénomènes sont bien réels, Momo réveille en elle un pouvoir caché tandis qu'une malédiction s'abat sur Okarun. Ensemble, ils doivent affronter les forces du paranormal qui menacent notre monde...",
                ZonedDateTime.parse("2024-10-03T16:00:00Z"),
                EpisodeType.EPISODE,
                "G619CPMQ1",
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
                mapOf(
                    ImageType.THUMBNAIL to "https://fr.web.img6.acsta.net/pictures/24/03/18/10/50/1581066.jpg",
                    ImageType.BANNER to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/fa62dd1fc7a9bc0b587f36f53bf572c1.jpg",
                ),
                "Entre menaces paranormales, nouveaux superpouvoirs et histoire d'amour naissante, deux lycéens se mettent au défi de prouver l'existence des fantômes ou des extraterrestres.",
                ZonedDateTime.parse("2024-10-03T16:00:00Z"),
                EpisodeType.EPISODE,
                "1",
                1,
                1,
                1440,
                "Serait-ce une romance qui commence ?",
                "Momo est une lycéenne qui croit aux fantômes, mais pas aux extraterrestres. Un jour, elle se dispute avec un camarade de classe qui croit aux ovnis, mais pas aux esprits.",
                "https://occ-0-8194-56.1.nflxso.net/dnm/api/v6/9pS1daC2n6UGc3dUogvWIPMR_OU/AAAABX1dtlp9gxfhu0LxOrbUq_MT-KlN7dEm6VpsU5JXCZoGzRx0VqYgzgKJa1EWZRnmym_WWv5CWg3jKqLdoYYX88X7IwSHEyeilqmA8ujaaGGeyTaH_A_fUm8D.webp",
                Platform.NETF,
                "ja-JP",
                "a7b9feca",
                "https://www.netflix.com/fr/title/81736884",
                uncensored = false,
                original = true,
            )
        )

        InvalidationService.invalidate(Anime::class.java, EpisodeMapping::class.java, EpisodeVariant::class.java)
        val animes = animeService.findAll()
        animes.forEach { println(it.name) }
        assertEquals(1, animes.size)
        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        val variants = episodeVariantService.findAll()
        assertEquals(4, variants.size)
    }

    @Test
    fun `save platform episode with rule`() {
        ruleService.save(Rule(platform = Platform.CRUN, seriesId = "GRMG8ZQZR", seasonId = "GYP8PM4KY", action = Rule.Action.REPLACE_ANIME_NAME, actionValue = "ONE PIECE Log: Fish-Man Island Saga"))
        ruleService.save(Rule(platform = Platform.CRUN, seriesId = "GRMG8ZQZR", seasonId = "GYP8PM4KY", action = Rule.Action.REPLACE_SEASON_NUMBER, actionValue = "1"))
        InvalidationService.invalidate(Rule::class.java)

        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "GRMG8ZQZR",
                "One Piece",
                mapOf(
                    ImageType.THUMBNAIL to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/757bae5a21039bac6ebace5de9affcd8.jpg",
                    ImageType.BANNER to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/a249096c7812deb8c3c2c907173f3774.jpg",
                ),
                "Embarquez pour le voyage de votre vie avec One Piece. La série épique créée par le célèbre mangaka Eiichiro Oda est un phénomène mondial qui a captivé le cœur des fans de toutes les générations au cours de ses 25 années d'existence. Cette aventure palpitante en haute mer est remplie d'histoire d'amitiés, de batailles épiques pour la liberté et de la poursuite de rêves. Rejoignez Monkey D. Luffy et son équipage de pirates attachants, alors qu'ils découvrent la véritable signification du pouvoir et de la justice dans cette grande ère de piraterie.\\r\\n\\r\\nMonkey D. Luffy refuse de laisser qui que ce soit ou quoi que ce soit se mettre en travers de sa quête pour devenir le Roi des pirates. Grâce à son pouvoir d'élasticité conféré par un Fruit du Démon, le jeune pirate fougueux cherche le trésor légendaire connu sous le nom de One Piece. Il mettra le cap sur les eaux périlleuses de Grand Line et recrutera l'équipage du Chapeau de paille. Ce capitaine ne jettera jamais l'ancre tant que ses amis et lui n'auront pas réalisé leurs rêves!\\r\\n\\r\\nOne Piece compte plus de 1100 épisodes. Actuellement, dans l'arc de l'île d'Egghead, l'équipage du Chapeau de paille rencontre enfin le très attendu Dr. Vegapunk. Crunchyroll propose tous les épisodes sous-titrés ainsi que tous les épisodes doublés en anglais, soit plus de 1000 épisodes. De plus, One Piece compte 13 épisodes hors-série et 15 films, dont le dernier, One Piece Film: Red, qui a connu un grand succès. One Piece est produit par Toei.",
                ZonedDateTime.parse("2024-12-08T08:00:00Z"),
                EpisodeType.EPISODE,
                "GYP8PM4KY",
                37,
                6,
                1430,
                "Paradis des sirènes ! Enfin l'île des hommes-poissons !",
                "L'équipage arrive enfin sur l'île des hommes-poissons, mais se retrouve séparé. Luffy, Usopp, Sanji et Chopper retrouvent Keimi. Cette dernière les amène à la baie des Sirènes, mais Sanji ne pouvant contrôler son excitation, l'équipage s'attire des problèmes.",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/6df155415e0da8a5b6a83effa7c29654.jpg",
                Platform.CRUN,
                "ja-JP",
                "GPWU8NDE8",
                "https://www.crunchyroll.com/fr/watch/GPWU8NDE8/mermaids-paradise-landing-at-fish-man-island",
                uncensored = false,
                original = true,
            )
        )

        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        assertEquals(EpisodeType.EPISODE, mappings.first().episodeType)
        assertEquals(1, mappings.first().season)
        assertEquals(6, mappings.first().number)
        assertEquals("one-piece-log", mappings.first().anime?.slug)
    }

    @Test
    fun `save platform episode with rule #2`() {
        ruleService.save(Rule(platform = Platform.CRUN, seriesId = "G649PJ0JY", seasonId = "G609CX8W1", action = Rule.Action.REPLACE_SEASON_NUMBER, actionValue = "4"))
        InvalidationService.invalidate(Rule::class.java)

        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "G649PJ0JY",
                "Blue Exorcist",
                mapOf(
                    ImageType.THUMBNAIL to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/cead08fd2ced6e6dbe056ce0381da6ff.jpg",
                    ImageType.BANNER to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/d172ce6deaf012f5e45863d3478854dd.jpg",
                ),
                "Vers la fin de l’ère Edo, un démon connu sous le nom de Roi Impur tua des milliers de personnes. Après avoir vaincu le démon, les Chevaliers de la Croix-Vraie mirent son œil gauche en sécurité dans l’Académie. Mais celui-ci a été volé ! En apprenant que le voleur retient un enfant en otage, Yukio et Rin décident de s’en mêler. L’enquête va alors mener Rin et ses amis à Kyoto et les faire plonger dans une histoire encore plus sinistre. Cependant, est-ce que le fait de savoir que Rin est le fils de Satan va semer la discorde dans le groupe ?",
                ZonedDateTime.parse("2024-12-07T18:00:00Z"),
                EpisodeType.EPISODE,
                "G609CX8W1",
                3,
                10,
                1420,
                "Sous la neige",
                "Tandis qu'un cyclope a fait son apparition en plein de centre de Tokyo, Yukio est plus que désespéré après s'en être pris à Ryûji. Et si c'était dans ces moments de solitude qu'il avait besoin de retrouver celle qui l'a toujours encouragé dans les moments difficiles ?",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/770b4a35184b971a8ecc086b4cb1610a.jpg",
                Platform.CRUN,
                "ja-JP",
                "G7PU3PVKP",
                "https://www.crunchyroll.com/fr/watch/G7PU3PVKP/in-the-falling-snow",
                uncensored = false,
                original = true,
            )
        )

        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        assertEquals(EpisodeType.EPISODE, mappings.first().episodeType)
        assertEquals(4, mappings.first().season)
        assertEquals(10, mappings.first().number)
        assertEquals("blue-exorcist", mappings.first().anime?.slug)
    }

    @Test
    fun `save platform episode with rule #3`() {
        ruleService.save(Rule(platform = Platform.ANIM, seriesId = "1166", seasonId = "1", action = Rule.Action.REPLACE_SEASON_NUMBER, actionValue = "2"))
        InvalidationService.invalidate(Rule::class.java)

        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "1166",
                "TONBO!",
                mapOf(
                    ImageType.THUMBNAIL to "https://image.animationdigitalnetwork.fr/license/tonbo/tv/web/affiche_350x500.jpg",
                    ImageType.BANNER to "https://image.animationdigitalnetwork.fr/license/tonbo/tv/web/license_640x360.jpg",
                ),
                "Igarashi, un ancien joueur professionnel cherchant à s’affranchir de son passé, s’établit à Hinoshima, une région isolée de la préfecture de Kagoshima. Sur place, il croise la route de Tonbo, la seule élève du collège de la péninsule, dotée d’un talent remarquable pour le golf. Cette rencontre fortuite entre deux passionnés de sport est sur le point de faire basculer le cours de leur existence…",
                ZonedDateTime.parse("2024-12-07T01:30:00Z"),
                EpisodeType.EPISODE,
                "1",
                1,
                23,
                1453,
                "Première fois sous pression",
                "Le dénouement du championnat approche, et chaque coup devient de plus en plus important à mesure que la tension monte. Pour la première fois, Tonbo doit lui faire face. Elle découvre ainsi le « véritable golf ».",
                "https://image.animationdigitalnetwork.fr/license/tonbo/tv/web/eps23_640x360.jpg",
                Platform.ANIM,
                "ja-JP",
                "25294",
                "https://animationdigitalnetwork.fr/video/tonbo/25294-episode-23-premiere-fois-sous-pression",
                uncensored = false,
                original = true,
            )
        )

        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        assertEquals(EpisodeType.EPISODE, mappings.first().episodeType)
        assertEquals(2, mappings.first().season)
        assertEquals(23, mappings.first().number)
        assertEquals("tonbo", mappings.first().anime?.slug)
    }

    @Test
    fun `save platform episode with rule #4`() {
        ruleService.save(Rule(platform = Platform.CRUN, seriesId = "GYQ43P3E6", seasonId = "G65VCD1KE", action = Rule.Action.ADD_TO_NUMBER, actionValue = "11"))
        InvalidationService.invalidate(Rule::class.java)

        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "GYQ43P3E6",
                "Black Butler",
                mapOf(
                    ImageType.THUMBNAIL to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/2ea138d06fb4b5dac870eb8379345f7c.jpg",
                    ImageType.BANNER to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/ff99c48c9aa901f08d3571a2276618a2.jpg",
                ),
                "Au sein de l'élite britannique, le collège Weston défie le gouvernement. Aussi, lorsque des étudiants disparaissent, dont le fils du cousin de la reine Victoria, Sa Majesté envoie le jeune comte Ciel Phantomhive pour enquêter. Accompagné de son fidèle majordome, Sebastian, Ciel doit naviguer dans les méandres de l'école et infiltrer les élites irréprochables de Weston, les P4, s'il veut élucider les mystères qui entourent cette institution.",
                ZonedDateTime.parse("2025-04-05T16:00:00Z"),
                EpisodeType.EPISODE,
                "G65VCD1KE",
                4,
                1,
                1419,
                "Le majordome enquête",
                "La reine Victoria envoie Ciel en Allemagne pour enquêter sur des morts étranges, par peur d’une épidémie.",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/3b941b231422355a359ddb1fb7e25cf1.jpg",
                Platform.CRUN,
                "ja-JP",
                "GQJUG5Z0Z",
                "https://www.crunchyroll.com/fr/watch/GQJUG5Z0Z/his-butler-doing-fieldwork",
                uncensored = false,
                original = true,
            )
        )

        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        assertEquals(EpisodeType.EPISODE, mappings.first().episodeType)
        assertEquals(4, mappings.first().season)
        assertEquals(12, mappings.first().number)
    }

    @Test
    fun `save platform episode with rule #5`() {
        ruleService.save(Rule(platform = Platform.CRUN, seriesId = "G1XHJV2X9", seasonId = "GRGGCVZV0", action = Rule.Action.REPLACE_SEASON_NUMBER, actionValue = "1"))
        ruleService.save(Rule(platform = Platform.CRUN, seriesId = "G1XHJV2X9", seasonId = "GRGGCVZV0", action = Rule.Action.REPLACE_EPISODE_TYPE, actionValue = "SPECIAL"))
        InvalidationService.invalidate(Rule::class.java)

        episodeVariantService.save(
            AbstractPlatform.Episode(
                CountryCode.FR,
                "G1XHJV2X9",
                "Lycoris Recoil",
                mapOf(
                    ImageType.THUMBNAIL to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/b407952122968b243e5c1e2b71d630d9.jpg",
                    ImageType.BANNER to "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/0b6e8bd4de40b260b3ccf0c700ce6710.jpg",
                ),
                "Si vous souhaitez avoir l’esprit tranquille, faites appel à Lycoris, une organisation secrète luttant contre le crime composée d'agents féminins. Avec son air insouciant, Chisato est énergique et la plus douée. Takina quant à elle semble plus mystérieuse et réservée, mais elle garde toujours la tête froide. Lorsqu’elles ne sont pas en mission, elles s’occupent d’un café qui propose des douceurs, s’adonnent au shopping et font même de la garde d’enfants ! Suivez le quotidien de ce drôle de duo.",
                ZonedDateTime.parse("2025-04-16T12:00:00Z"),
                EpisodeType.EPISODE,
                "GRGGCVZV0",
                11,
                1,
                135,
                "Relax",
                "Au LycoReco, tout le monde s’affaire pour fêter la floraison des cerisiers !",
                "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/b9f3e08145a45aaa7ebe3f303a7413ee.jpg",
                Platform.CRUN,
                "ja-JP",
                "G2XUN17ZJ",
                "https://www.crunchyroll.com/fr/watch/G2XUN17ZJ/take-it-easy",
                uncensored = false,
                original = true,
            )
        )

        val mappings = episodeMappingService.findAll()
        assertEquals(1, mappings.size)
        assertEquals(EpisodeType.SPECIAL, mappings.first().episodeType)
        assertEquals(1, mappings.first().season)
        assertEquals(1, mappings.first().number)
    }
}