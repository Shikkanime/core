package fr.shikkanime.socialnetworks

import com.google.inject.Inject
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.EpisodeDto
import fr.shikkanime.dtos.SimulcastDto
import fr.shikkanime.dtos.enums.Status
import fr.shikkanime.entities.Config
import fr.shikkanime.entities.enums.*
import fr.shikkanime.repositories.ConfigRepository
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class TwitterSocialNetworkTest {
    @Inject
    private lateinit var twitterSocialNetwork: TwitterSocialNetwork

    @Inject
    private lateinit var configRepository: ConfigRepository

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)

        configRepository.save(
            Config(
                propertyKey = ConfigPropertyKey.TWITTER_MESSAGE.key,
                propertyValue = "🚨 {EPISODE_INFORMATION} de {ANIME_HASHTAG} est maintenant disponible{VOICE}sur {PLATFORM_ACCOUNT}\n\nBon visionnage. 🍿\n\n🔶 Lien de l'épisode : {SHIKKANIME_URL}",

                )
        )

        MapCache.invalidate(Config::class.java)
    }

    @AfterEach
    fun tearDown() {
        configRepository.findByName(ConfigPropertyKey.TWITTER_MESSAGE.key)?.let { configRepository.delete(it) }
        MapCache.invalidate(Config::class.java)
    }

    @Test
    fun getMessage1() {
        val episodeDto = EpisodeDto(
            uuid = UUID.fromString("c2d2759a-8475-4ba1-9554-3c721c8b281f"),
            platform = Platform.CRUN,
            anime = AnimeDto(
                uuid = UUID.fromString("c2d2759a-8475-4ba1-9554-3c721c8b281f"),
                countryCode = CountryCode.FR,
                name = "The Foolish Angel Dances with the Devil",
                shortName = "The Foolish Angel Dances with the Devil",
                releaseDateTime = "2024-01-08T18:00:00Z",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/44b38d6f3cc6e0a97006bdac5b139ea5.jpe",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/18187c3c5bb45febea51842b9748d2b2.jpe",
                description = "Déterminé à protéger son monde démoniaque des anges célestes, le démon Masatora Akutsu se rend sur Terre à la recherche d'une humaine afin d’augmenter ses effectifs. Se faisant passer pour un lycéen durant sa mission de recrutement, il est captivé par la charmante Lily Amane. Elle est pourtant l’un de ses ennemis jurés (un ange) et bien décidée à lui faire oublier ses réflexes démoniaques !",
                simulcasts = listOf(
                    SimulcastDto(
                        uuid = UUID.fromString("d1993c0c-4f65-474b-9c04-c577435aa7d5"),
                        season = "WINTER",
                        year = 2024,
                        slug = "winter-2024",
                        label = "Hiver 2024",
                    )
                ),
                status = Status.VALID,
                slug = "the-foolish-angel-dances-with-the-devil",
                lastReleaseDateTime = "2024-02-26T18:00:00Z",
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "FR-CRUN-918505-SUBTITLES",
            releaseDateTime = "2024-02-26T18:00:00Z",
            season = 1,
            number = 8,
            title = "Deux idiots en désaccord",
            url = "https://www.crunchyroll.com/media-918505",
            image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/bc972b9a51f735d67b763edf2f690708.jpe",
            duration = 1420,
            description = "Comme ils ont été vus sortant ensemble du même immeuble le matin, Akutsu et Amane sont instantanément devenus l'objet de toutes les rumeurs. Il va maintenant falloir dissiper le malentendu.",
            uncensored = false,
            lastUpdateDateTime = "2024-02-26T18:00:00Z",
            status = Status.VALID,
        )

        assertEquals(
            "\uD83D\uDEA8 L'épisode 8 de #TheFoolishAngelDancesWithTheDevil est maintenant disponible sur @Crunchyroll_fr\n" +
                    "\n" +
                    "Bon visionnage. \uD83C\uDF7F\n" +
                    "\n" +
                    "\uD83D\uDD36 Lien de l'épisode : https://www.shikkanime.fr/animes/the-foolish-angel-dances-with-the-devil",
            twitterSocialNetwork.getMessage(episodeDto)
        )
    }

    @Test
    fun getMessage2() {
        val episodeDto = EpisodeDto(
            uuid = UUID.fromString("e858f537-9a15-4dd6-a1a5-d2d26c760413"),
            platform = Platform.CRUN,
            anime = AnimeDto(
                uuid = UUID.fromString("e32b99b1-a7a8-4785-9586-f079355f9502"),
                countryCode = CountryCode.FR,
                name = "Shangri-La Frontier",
                shortName = "Shangri-La Frontier",
                releaseDateTime = "2024-01-07T10:30:00Z",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/480x720/catalog/crunchyroll/a2f948157077e3d65471329d9dd43be1.jpe",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/34cf65c200da726462d0c0ac7e0e55af.jpe",
                description = "Dans un futur proche, les jeux s’affichant sur des écrans sont passés de mode. La technologie VR en immersion domine ce marché, mais parmi la multitude de jeux, un grand nombre sont bien souvent nuls et sans intérêt. Certains ont décidé de s’attaquer à ces jeux, comme Rakuro Hizutome, alias Sunraku, qui se plaît à enchaîner les pires d’entre-eux. Mais cette fois, il a décidé de se lancer dans Shangri-La Frontier, un jeu grand public aux quelques trente millions de membres inscrits ! Des amis en ligne, un univers immense, des rencontres avec des rivaux... Rakuro est loin d’imaginer que sa vie, voire son destin et celui de nombreuses personnes, est sur le point de changer lorsqu’il se lance dans cette aventure…",
                simulcasts = listOf(
                    SimulcastDto(
                        uuid = UUID.fromString("d1993c0c-4f65-474b-9c04-c577435aa7d5"),
                        season = "WINTER",
                        year = 2024,
                        slug = "winter-2024",
                        label = "Hiver 2024",
                    )
                ),
                status = Status.VALID,
                slug = "shangri-la-frontier",
                lastReleaseDateTime = "2024-02-25T20:00:00Z",
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.VOICE,
            hash = "FR-CRUN-922508-VOICE",
            releaseDateTime = "2024-02-25T20:00:00Z",
            season = 1,
            number = 16,
            title = "Émotions pour un bref instant (partie 2)",
            url = "https://www.crunchyroll.com/media-922508",
            image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/8c314cd552392412f3a6ae5c54a0ab67.jpe",
            duration = 1578,
            description = "Sunraku, Katzo et Pencilgon ont entamé leur combat contre Wethermon. Leur objectif : survivre dix minutes afin d’atteindre la deuxième phase du boss. Mais celui-ci s’avère redoutable et force l’équipe à utiliser tous ses objets de soin.",
            uncensored = false,
            lastUpdateDateTime = "2024-02-25T20:00:00Z",
            status = Status.VALID,
        )

        assertEquals(
            "\uD83D\uDEA8 L'épisode 16 de #ShangriLaFrontier est maintenant disponible en VF sur @Crunchyroll_fr\n" +
                    "\n" +
                    "Bon visionnage. \uD83C\uDF7F\n" +
                    "\n" +
                    "\uD83D\uDD36 Lien de l'épisode : https://www.shikkanime.fr/animes/shangri-la-frontier", twitterSocialNetwork.getMessage(episodeDto)
        )
    }

    @Test
    fun getMessage3() {
        val episodeDto = EpisodeDto(
            uuid = UUID.fromString("ca116f60-51df-4590-9670-de0adb118c27"),
            platform = Platform.ANIM,
            anime = AnimeDto(
                uuid = UUID.fromString("fe2eb550-2821-4a82-ae1c-e3f11deb61f6"),
                countryCode = CountryCode.FR,
                name = "Looking up to Magical Girls",
                shortName = "Looking up to Magical Girls",
                releaseDateTime = "2024-01-03T15:30:00Z",
                image = "https://image.animationdigitalnetwork.fr/license/mahoushoujoniakogarete/tv/web/affiche_350x500.jpg",
                banner = "https://image.animationdigitalnetwork.fr/license/mahoushoujoniakogarete/tv/web/license_640x360.jpg",
                description = "Fan de magical girls depuis sa plus tendre enfance, Utena Hiiragi rêve de combattre auprès des Tres Magia, le trio protégeant sa ville. Alors, quand un adorable démon lui propose de la métamorphoser, l’adolescente accepte immédiatement. Cependant, son enthousiasme est de courte durée lorsqu’elle prend conscience qu’elle est désormais enrôlée au sein d’Enormita, une organisation maléfique contre laquelle luttent ses justicières favorites ! Si Utena est dans un premier temps réticente à s’engager dans ce conflit, elle doit faire face à une troublante révélation : torturer ses idoles lui procure un immense plaisir.",
                simulcasts = listOf(
                    SimulcastDto(
                        uuid = UUID.fromString("d1993c0c-4f65-474b-9c04-c577435aa7d5"),
                        season = "WINTER",
                        year = 2024,
                        slug = "winter-2024",
                        label = "Hiver 2024",
                    )
                ),
                status = Status.VALID,
                slug = "looking-up-to-magical-girls",
                lastReleaseDateTime = "2024-02-21T15:30:00Z",
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "FR-ANIM-24154-SUBTITLES",
            releaseDateTime = "2024-02-21T15:30:00Z",
            season = 1,
            number = 8,
            title = "L'Apparition du groupe Lord",
            url = "https://animationdigitalnetwork.fr/video/looking-up-to-magical-girls/24154-episode-8-nc-auftritt-der-lord-truppe",
            image = "https://image.animationdigitalnetwork.fr/license/mahoushoujoniakogaretenc/tv/web/eps8_640x360.jpg",
            duration = 1419,
            description = "Utena, Kiwi et Korisu se rendent à Nacht base et font la connaissance de Lord Enorme, Loco Musica, Sister Gigant et Leberblume, les membres historiques d’Enormita. Cette rencontre va prendre une tournure des plus inattendues…",
            uncensored = true,
            lastUpdateDateTime = "2024-02-21T15:30:00Z",
            status = Status.VALID,
        )

        assertEquals(
            "\uD83D\uDEA8 L'épisode 8 non censuré de #LookingUpToMagicalGirls est maintenant disponible sur @ADNanime\n" +
                    "\n" +
                    "Bon visionnage. \uD83C\uDF7F\n" +
                    "\n" +
                    "\uD83D\uDD36 Lien de l'épisode : https://www.shikkanime.fr/animes/looking-up-to-magical-girls", twitterSocialNetwork.getMessage(episodeDto)
        )
    }

    @Test
    fun getMessage4() {
        configRepository.findByName(ConfigPropertyKey.TWITTER_MESSAGE.key)?.let { configRepository.delete(it) }
        configRepository.save(
            Config(
                propertyKey = ConfigPropertyKey.TWITTER_MESSAGE.key,
                propertyValue = "Nouveau ! {EPISODE_INFORMATION} de {ANIME_HASHTAG} est dispo{VOICE}sur {PLATFORM_ACCOUNT} !\n" +
                        "\n" +
                        "Ne manquez pas la suite des aventures de {ANIME_TITLE} !\n" +
                        "\n" +
                        "➡\uFE0F Lien : {URL}\n" +
                        "\n" +
                        "#anime"
            )
        )
        MapCache.invalidate(Config::class.java)

        val episodeDto = EpisodeDto(
            uuid = UUID.fromString("ca116f60-51df-4590-9670-de0adb118c27"),
            platform = Platform.ANIM,
            anime = AnimeDto(
                uuid = UUID.fromString("fe2eb550-2821-4a82-ae1c-e3f11deb61f6"),
                countryCode = CountryCode.FR,
                name = "Looking up to Magical Girls",
                shortName = "Looking up to Magical Girls",
                releaseDateTime = "2024-01-03T15:30:00Z",
                image = "https://image.animationdigitalnetwork.fr/license/mahoushoujoniakogarete/tv/web/affiche_350x500.jpg",
                banner = "https://image.animationdigitalnetwork.fr/license/mahoushoujoniakogarete/tv/web/license_640x360.jpg",
                description = "Fan de magical girls depuis sa plus tendre enfance, Utena Hiiragi rêve de combattre auprès des Tres Magia, le trio protégeant sa ville. Alors, quand un adorable démon lui propose de la métamorphoser, l’adolescente accepte immédiatement. Cependant, son enthousiasme est de courte durée lorsqu’elle prend conscience qu’elle est désormais enrôlée au sein d’Enormita, une organisation maléfique contre laquelle luttent ses justicières favorites ! Si Utena est dans un premier temps réticente à s’engager dans ce conflit, elle doit faire face à une troublante révélation : torturer ses idoles lui procure un immense plaisir.",
                simulcasts = listOf(
                    SimulcastDto(
                        uuid = UUID.fromString("d1993c0c-4f65-474b-9c04-c577435aa7d5"),
                        season = "WINTER",
                        year = 2024,
                        slug = "winter-2024",
                        label = "Hiver 2024",
                    )
                ),
                status = Status.VALID,
                slug = "looking-up-to-magical-girls",
                lastReleaseDateTime = "2024-02-21T15:30:00Z",
            ),
            episodeType = EpisodeType.EPISODE,
            langType = LangType.SUBTITLES,
            hash = "FR-ANIM-24154-SUBTITLES",
            releaseDateTime = "2024-02-21T15:30:00Z",
            season = 1,
            number = 8,
            title = "L'Apparition du groupe Lord",
            url = "https://animationdigitalnetwork.fr/video/looking-up-to-magical-girls/24154-episode-8-nc-auftritt-der-lord-truppe",
            image = "https://image.animationdigitalnetwork.fr/license/mahoushoujoniakogaretenc/tv/web/eps8_640x360.jpg",
            duration = 1419,
            description = "Utena, Kiwi et Korisu se rendent à Nacht base et font la connaissance de Lord Enorme, Loco Musica, Sister Gigant et Leberblume, les membres historiques d’Enormita. Cette rencontre va prendre une tournure des plus inattendues…",
            uncensored = true,
            lastUpdateDateTime = "2024-02-21T15:30:00Z",
            status = Status.VALID,
        )

        assertEquals(
            "Nouveau ! L'épisode 8 non censuré de #LookingUpToMagicalGirls est dispo sur @ADNanime !\n" +
                    "\n" +
                    "Ne manquez pas la suite des aventures de Looking up to Magical Girls !\n" +
                    "\n" +
                    "➡\uFE0F Lien : https://animationdigitalnetwork.fr/video/looking-up-to-magical-girls/24154-episode-8-nc-auftritt-der-lord-truppe\n" +
                    "\n" +
                    "#anime", twitterSocialNetwork.getMessage(episodeDto)
        )
    }
}