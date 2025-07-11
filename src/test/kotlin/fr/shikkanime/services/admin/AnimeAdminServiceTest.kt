package fr.shikkanime.services.admin

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import fr.shikkanime.dtos.GenericDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.factories.impl.AnimeFactory
import fr.shikkanime.utils.StringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class AnimeAdminServiceTest : AbstractTest() {
    @Inject private lateinit var animeFactory: AnimeFactory
    @Inject private lateinit var animeAdminService: AnimeAdminService

    @Test
    fun `update with no episodes`() {
        animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "DRAGON QUEST The Adventure of Dai",
                slug = "dragon-quest-the-adventure-of-dai"
            )
        )

        animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "Dragon Quest",
                slug = "dragon-quest"
            )
        )

        val anime = animeService.findByName(CountryCode.FR, "DRAGON QUEST The Adventure of Dai")
        assertNotNull(anime)
        val dto = animeFactory.toDto(anime)
        dto.slug = "dragon-quest"

        animeAdminService.update(anime.uuid!!, dto)

        assertEquals(1, animeService.findAll().size)
        assertEquals("Dragon Quest", animeService.findAll()[0].name)
    }

    @Test
    fun `update with episodes`() {
        val anime1 = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "DRAGON QUEST The Adventure of Dai",
                slug = "dragon-quest-the-adventure-of-dai"
            )
        )

        val mapping1 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime1,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1,
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
                slug = "dragon-quest"
            )
        )

        val mapping2 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime2,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1,
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
        assertNotNull(anime)
        val dto = animeFactory.toDto(anime)
        dto.slug = "dragon-quest"

        animeAdminService.update(anime.uuid!!, dto)

        assertEquals(1, animeService.findAll().size)
        assertEquals("Dragon Quest", animeService.findAll()[0].name)
        assertEquals(1, episodeMappingService.findAll().size)
        assertEquals(3, episodeVariantService.findAll().size)
    }

    @Test
    fun `update with episodes and follow`() {
        val member = memberService.register(StringUtils.generateRandomString(12))

        val anime1 = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                name = "DRAGON QUEST The Adventure of Dai",
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
            )
        )

        memberFollowEpisodeService.follow(member.uuid, GenericDto(mapping1.uuid!!))

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
                slug = "dragon-quest"
            )
        )

        memberFollowAnimeService.follow(member.uuid, GenericDto(anime2.uuid!!))

        val mapping2 = episodeMappingService.save(
            EpisodeMapping(
                anime = anime2,
                episodeType = EpisodeType.EPISODE,
                season = 1,
                number = 1,
            )
        )

        memberFollowEpisodeService.follow(member.uuid, GenericDto(mapping2.uuid!!))

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
            )
        )

        memberFollowEpisodeService.follow(member.uuid, GenericDto(mapping3.uuid!!))

        assertEquals(1, memberService.findAll().size)
        assertEquals(2, memberFollowAnimeService.findAll().size)
        assertEquals(3, memberFollowEpisodeService.findAll().size)
        assertEquals(2, animeService.findAll().size)
        assertEquals(3, episodeMappingService.findAll().size)
        assertEquals(3, episodeVariantService.findAll().size)

        val anime = animeService.findByName(CountryCode.FR, "DRAGON QUEST The Adventure of Dai")
        assertNotNull(anime)
        val dto = animeFactory.toDto(anime)
        dto.slug = "dragon-quest"

        animeAdminService.update(anime.uuid!!, dto)

        assertEquals(1, animeService.findAll().size)
        assertEquals("Dragon Quest", animeService.findAll()[0].name)
        assertEquals(2, episodeMappingService.findAll().size)
        assertEquals(3, episodeVariantService.findAll().size)
        assertEquals(1, memberService.findAll().size)
        assertEquals(1, memberFollowAnimeService.findAll().size)
        assertEquals(2, memberFollowEpisodeService.findAll().size)
    }
}