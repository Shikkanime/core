package fr.shikkanime.socialnetworks

import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.miscellaneous.GroupedEpisode
import fr.shikkanime.utils.Constant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*

class SocialNetworkTextTest : AbstractTest() {
    private val socialNetwork = object : AbstractSocialNetwork() {
        override val priority: Int = 0
        override fun login() {}
        override fun logout() {}
        override suspend fun sendEpisodeRelease(groupedEpisodes: List<GroupedEpisode>, mediaImage: ByteArray?) {}
    }

    private fun createGroupedEpisode(animeName: String, episodeNumber: Int, audioLocale: String = "ja-JP"): GroupedEpisode {
        val anime = Anime(name = animeName, countryCode = CountryCode.FR, uuid = UUID.randomUUID())
        val variant = EpisodeVariant(
            uuid = UUID.randomUUID(),
            audioLocale = audioLocale,
            url = "https://shikkanime.fr/r/${UUID.randomUUID()}"
        )
        return GroupedEpisode(
            anime = anime,
            releaseDateTime = ZonedDateTime.now(),
            lastUpdateDateTime = ZonedDateTime.now(),
            minSeason = 1,
            maxSeason = 1,
            episodeType = EpisodeType.EPISODE,
            minNumber = episodeNumber,
            maxNumber = episodeNumber,
            mappings = setOf(UUID.randomUUID()),
            variants = listOf(variant)
        )
    }

    @Test
    fun testSingleEpisodeMessage() {
        setUp()
        val ge = createGroupedEpisode("One Piece", 1000)
        val baseMessage = "{ANIME_TITLE} : {EPISODE_INFORMATION} {VOICE} {BE} {AVAILABLE} sur {SHIKKANIME_URL} {ANIME_HASHTAG}"
        val message = socialNetwork.getEpisodeMessage(listOf(ge), baseMessage)
        
        val expected = "One Piece : L'épisode 1000 en VOSTFR est disponible sur ${Constant.baseUrl}/r/${ge.variants.first().uuid} #OnePiece"
        assertEquals(expected, message)
    }

    @Test
    fun testSingleEpisodeMessageWithNewPlaceholders() {
        setUp()
        val ge = createGroupedEpisode("One Piece", 1000)
        ge.title = "Un titre génial"
        ge.anime.description = "Une description longue"
        val baseMessage = "{ANIME_TITLE} - {EPISODE_TITLE}\n{EPISODE_INFORMATION} de S{SEASON_NUMBER}E{EPISODE_NUMBER}\n{ANIME_DESCRIPTION}"
        val message = socialNetwork.getEpisodeMessage(listOf(ge), baseMessage)

        val expected = "One Piece - Un titre génial\nL'épisode 1000 de S1E1000\nUne description longue"
        assertEquals(expected, message)
    }

    @Test
    fun testMultipleEpisodesMessage() {
        setUp()
        val ge1 = createGroupedEpisode("One Piece", 1000)
        val ge2 = createGroupedEpisode("Naruto", 500, "fr-FR")
        val baseMessage = "{COUNT} épisodes dispos sur {SHIKKANIME_URL} :\n{EPISODES_LIST}\n\nOn a du {ANIME_TITLES} !"
        val message = socialNetwork.getEpisodeMessage(listOf(ge1, ge2), baseMessage)
        
        val expected = "2 épisodes dispos sur ${Constant.baseUrl} :\n• One Piece : L'épisode 1000 (VOSTFR)\n• Naruto : L'épisode 500 (VF)\n\nOn a du One Piece & Naruto !"
        assertEquals(expected, message)
    }

    @Test
    fun testMultipleEpisodesMessageWithCustomListTemplate() {
        setUp()
        val ge1 = createGroupedEpisode("One Piece", 1000)
        val ge2 = createGroupedEpisode("Naruto", 500, "fr-FR")
        val baseMessage = "{COUNT} épisodes :\n{EPISODES_LIST[- {ANIME_TITLE} ({LANG}) -> {SHIKKANIME_URL}]}"
        val message = socialNetwork.getEpisodeMessage(listOf(ge1, ge2), baseMessage)

        val expected = "2 épisodes :\n- One Piece (VOSTFR) -> ${Constant.baseUrl}/r/${ge1.variants.first().uuid}\n- Naruto (VF) -> ${Constant.baseUrl}/r/${ge2.variants.first().uuid}"
        assertEquals(expected, message)
    }

    @Test
    fun testSingleEpisodeMessageWithLangPlaceholder() {
        setUp()
        val ge = createGroupedEpisode("One Piece", 1000)
        val baseMessage = "{ANIME_TITLE} est dispo en {LANG} !"
        val message = socialNetwork.getEpisodeMessage(listOf(ge), baseMessage)

        val expected = "One Piece est dispo en VOSTFR !"
        assertEquals(expected, message)
    }

    @Test
    fun testMultipleEpisodesMessageWithCustomListTemplate2() {
        setUp()
        val ge1 = createGroupedEpisode("One Piece", 1000)
        val ge2 = createGroupedEpisode("Naruto", 500, "fr-FR")
        val baseMessage = "🍿 {COUNT} nouveaux épisodes sont disponibles !\n\n{EPISODES_LIST[• {EPISODE_INFORMATION} de {ANIME_TITLE} {VOICE}]}"
        val message = socialNetwork.getEpisodeMessage(listOf(ge1, ge2), baseMessage)

        val expected = "🍿 2 nouveaux épisodes sont disponibles !\n\n• L'épisode 1000 de One Piece en VOSTFR\n• L'épisode 500 de Naruto en VF"
        assertEquals(expected, message)
    }
}
