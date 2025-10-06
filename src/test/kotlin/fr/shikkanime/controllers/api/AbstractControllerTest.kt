package fr.shikkanime.controllers.api

import fr.shikkanime.AbstractTest
import fr.shikkanime.dtos.member.MemberDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.InvalidationService
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.ZonedDateTime

abstract class AbstractControllerTest : AbstractTest() {
    @BeforeEach
    override fun setUp() {
        Constant.injector.injectMembers(this)

        initOnePiece()
        init7thTimeLoop()
        animeService.recalculateSimulcasts()
        episodeVariantService.preIndex()
        InvalidationService.invalidateAll()
    }

    protected suspend fun ApplicationTestBuilder.registerAndLogin(): Pair<String, String> {
        var identifier: String?

        client.post("/api/v1/members/register").apply {
            assertEquals(HttpStatusCode.Created, status)
            identifier = ObjectParser.fromJson(bodyAsText(), Map::class.java)["identifier"].toString()
            val findPrivateMember = memberService.findByIdentifier(identifier)
            assertNotNull(findPrivateMember)
            assertTrue(findPrivateMember!!.isPrivate)
        }

        client.post("/api/v1/members/login") {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                append("X-App-Version", "1.0.0+1")
                append("X-Device", "android")
                append("X-Locale", "fr_FR")
            }
            setBody(identifier!!)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val tokenDto = ObjectParser.fromJson(bodyAsText(), MemberDto::class.java)
            assertTrue(tokenDto.token.isNotBlank())
            return identifier!! to tokenDto.token
        }
    }

    private fun initOnePiece() {
        val firstReleaseDateTime = ZonedDateTime.parse("2021-10-24T07:00:00Z")

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "One Piece",
                slug = "one-piece",
                releaseDateTime = firstReleaseDateTime,
                description = "Luffy et son équipage naviguent sur les mers à la recherche du légendaire trésor appelé « One Piece » et doivent faire face à de nombreuses aventures. Celui qui s’en emparera deviendra le roi des pirates, rêve ultime de Luffy.",
            )
        )

        // 995 -> 1110

        (995..1110).forEachIndexed { index, episodeNumber ->
            val releaseDateTime = firstReleaseDateTime.plusWeeks(index.toLong())

            val mapping = episodeMappingService.save(
                EpisodeMapping(
                    anime = anime,
                    releaseDateTime = releaseDateTime,
                    episodeType = EpisodeType.EPISODE,
                    season = 1,
                    number = episodeNumber,
                    duration = 1440,
                    title = "Episode $episodeNumber",
                    description = "Description $episodeNumber",
                )
            )

            episodeVariantService.save(
                EpisodeVariant(
                    mapping = mapping,
                    releaseDateTime = releaseDateTime,
                    platform = Platform.ANIM,
                    audioLocale = "ja-JP",
                    identifier = "FR-ANIM-ONE-PIECE-${episodeNumber}-JA-JP",
                    url = "https://animationdigitalnetwork.fr/video/one-piece-saga-14-pays-de-wano/17889-episode-1000-puissance-hors-du-commun-l-equipage-du-chapeau-de-paille-au-complet",
                )
            )

            episodeVariantService.save(
                EpisodeVariant(
                    mapping = mapping,
                    releaseDateTime = releaseDateTime.plusMinutes(10),
                    platform = Platform.ANIM,
                    audioLocale = "fr-FR",
                    identifier = "FR-ANIM-ONE-PIECE-${episodeNumber}-FR-FR",
                    url = "https://animationdigitalnetwork.fr/video/one-piece-saga-14-pays-de-wano/17889-episode-1000-puissance-hors-du-commun-l-equipage-du-chapeau-de-paille-au-complet",
                )
            )

            episodeVariantService.save(
                EpisodeVariant(
                    mapping = mapping,
                    releaseDateTime = releaseDateTime,
                    platform = Platform.CRUN,
                    audioLocale = "ja-JP",
                    identifier = "FR-CRUN-ONE-PIECE-${episodeNumber}-JA-JP",
                    url = "https://www.crunchyroll.com/fr/watch/G2XU03VQ5/overwhelming-strength-the-straw-hats-come-together",
                )
            )
        }
    }

    private fun init7thTimeLoop() {
        val firstReleaseDateTime = ZonedDateTime.parse("2024-01-07T15:15:00Z")

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "7th Time Loop: The Villainess Enjoys a Carefree Life Married to Her Worst Enemy!",
                slug = "7th-time-loop",
                releaseDateTime = firstReleaseDateTime,
                description = "Rishe, la fille du duc, n'est pas étrangère à la réincarnation - c'est sa septième vie, après tout. Chaque vie recommence à ses fiançailles rompues. Après avoir été marchande, servante et chevalier, elle aspire maintenant aux loisirs. Mais son monde change lorsqu'un prince, qui l'a tuée dans une vie antérieure, la demande en mariage ! Pour éviter la guerre et vivre jusqu'à un âge avancé, elle commence sa septième vie en tant qu'épouse du prince d'une nation ennemie.",
            )
        )

        // 1 -> 12

        (1..12).forEachIndexed { index, episodeNumber ->
            val releaseDateTime = firstReleaseDateTime.plusWeeks(index.toLong())

            val mapping = episodeMappingService.save(
                EpisodeMapping(
                    anime = anime,
                    releaseDateTime = releaseDateTime,
                    episodeType = EpisodeType.EPISODE,
                    season = 1,
                    number = episodeNumber,
                    duration = 1430,
                    title = "Episode $episodeNumber",
                    description = "Description $episodeNumber",
                )
            )

            episodeVariantService.save(
                EpisodeVariant(
                    mapping = mapping,
                    releaseDateTime = releaseDateTime,
                    platform = Platform.CRUN,
                    audioLocale = "ja-JP",
                    identifier = "FR-CRUN-7TH-TIME-LOOP-${episodeNumber}-JA-JP",
                    url = "https://www.crunchyroll.com/fr/watch/GMKUXP0JX/the-fianc-who-killed-me",
                )
            )
        }
    }
}