package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.*
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnimeServiceTest {
    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeMappingService: EpisodeMappingService

    @Inject
    private lateinit var episodeVariantService: EpisodeVariantService

    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var memberFollowAnimeService: MemberFollowAnimeService

    @Inject
    private lateinit var memberFollowEpisodeService: MemberFollowEpisodeService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)
    }

    @AfterEach
    fun tearDown() {
        memberFollowEpisodeService.deleteAll()
        memberFollowAnimeService.deleteAll()
        memberService.deleteAll()
        episodeVariantService.deleteAll()
        episodeMappingService.deleteAll()
        animeService.deleteAll()
        MapCache.invalidate(
            Anime::class.java,
            EpisodeMapping::class.java,
            EpisodeVariant::class.java,
            Member::class.java,
            MemberFollowAnime::class.java,
            MemberFollowEpisode::class.java
        )
    }

    @Test
    fun `update with no episodes`() {
        animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "DRAGON QUEST The Adventure of Dai",
                image = "test.jpg",
                slug = "dragon-quest-the-adventure-of-dai"
            )
        )

        animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Dragon Quest",
                image = "test.jpg",
                slug = "dragon-quest"
            )
        )

        val anime = animeService.findByName(CountryCode.FR, "DRAGON QUEST The Adventure of Dai")
        val dto = AbstractConverter.convert(anime, AnimeDto::class.java)
        dto.slug = "dragon-quest"

        animeService.update(anime!!.uuid!!, dto)

        assertEquals(1, animeService.findAll().size)
    }

    @Test
    fun `update with episodes`() {
        val anime1 = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "DRAGON QUEST The Adventure of Dai",
                image = "test.jpg",
                slug = "dragon-quest-the-adventure-of-dai"
            )
        )

        val mapping1 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime1,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1,
                image = "test.jpg",
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = mapping1,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "test-1-CRUN",
                url = "test-1-CRUN.mp4"
            )
        )

        val anime2 = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Dragon Quest",
                image = "test.jpg",
                slug = "dragon-quest"
            )
        )

        val mapping2 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime2,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1,
                image = "test.jpg",
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = mapping2,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "test-1-ANIM",
                url = "test-1-ANIM.mp4"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = mapping2,
                platform = Platform.ANIM,
                audioLocale = "fr-FR",
                identifier = "test-1-FR-ANIM",
                url = "test-1-FR-ANIM.mp4"
            )
        )

        val anime = animeService.findByName(CountryCode.FR, "DRAGON QUEST The Adventure of Dai")
        val dto = AbstractConverter.convert(anime, AnimeDto::class.java)
        dto.slug = "dragon-quest"

        animeService.update(anime!!.uuid!!, dto)

        assertEquals(1, animeService.findAll().size)
        assertEquals(1, episodeMappingService.findAll().size)
        assertEquals(3, episodeVariantService.findAll().size)
    }

    @Test
    fun `update with episodes and follow`() {
        val member = memberService.save(StringUtils.generateRandomString(12))

        val anime1 = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "DRAGON QUEST The Adventure of Dai",
                image = "test.jpg",
                slug = "dragon-quest-the-adventure-of-dai"
            )
        )

        memberFollowAnimeService.follow(member.uuid!!, GenericDto(anime1.uuid!!))

        val mapping1 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime1,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1,
                image = "test.jpg",
            )
        )

        memberFollowEpisodeService.follow(member.uuid!!, GenericDto(mapping1.uuid!!))

        episodeVariantService.save(
            EpisodeVariant(
                mapping = mapping1,
                platform = Platform.CRUN,
                audioLocale = "ja-JP",
                identifier = "test-1-CRUN",
                url = "test-1-CRUN.mp4"
            )
        )

        val anime2 = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Dragon Quest",
                image = "test.jpg",
                slug = "dragon-quest"
            )
        )

        memberFollowAnimeService.follow(member.uuid!!, GenericDto(anime2.uuid!!))

        val mapping2 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime2,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1,
                image = "test.jpg",
            )
        )

        memberFollowEpisodeService.follow(member.uuid!!, GenericDto(mapping2.uuid!!))

        episodeVariantService.save(
            EpisodeVariant(
                mapping = mapping2,
                platform = Platform.ANIM,
                audioLocale = "ja-JP",
                identifier = "test-1-ANIM",
                url = "test-1-ANIM.mp4"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = mapping2,
                platform = Platform.ANIM,
                audioLocale = "fr-FR",
                identifier = "test-1-FR-ANIM",
                url = "test-1-FR-ANIM.mp4"
            )
        )

        val mapping3 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime2,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 2,
                image = "test.jpg",
            )
        )

        memberFollowEpisodeService.follow(member.uuid!!, GenericDto(mapping3.uuid!!))

        assertEquals(1, memberService.findAll().size)
        assertEquals(2, memberFollowAnimeService.findAll().size)
        assertEquals(3, memberFollowEpisodeService.findAll().size)
        assertEquals(2, animeService.findAll().size)
        assertEquals(3, episodeMappingService.findAll().size)
        assertEquals(3, episodeVariantService.findAll().size)

        val anime = animeService.findByName(CountryCode.FR, "DRAGON QUEST The Adventure of Dai")
        val dto = AbstractConverter.convert(anime, AnimeDto::class.java)
        dto.slug = "dragon-quest"

        animeService.update(anime!!.uuid!!, dto)

        assertEquals(1, animeService.findAll().size)
        assertEquals(2, episodeMappingService.findAll().size)
        assertEquals(3, episodeVariantService.findAll().size)
        assertEquals(1, memberService.findAll().size)
        assertEquals(1, memberFollowAnimeService.findAll().size)
        assertEquals(2, memberFollowEpisodeService.findAll().size)
    }
}