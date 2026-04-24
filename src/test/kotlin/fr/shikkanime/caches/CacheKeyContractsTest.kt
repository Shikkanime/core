package fr.shikkanime.caches

import fr.shikkanime.caches.contracts.CountryPageableCacheKey
import fr.shikkanime.caches.contracts.SearchTypesCacheKey
import fr.shikkanime.caches.contracts.SortParametersCacheKey
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.miscellaneous.SortParameter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class CacheKeyContractsTest {
    @Test
    fun `countryCode sort pagination key compares arrays by content`() {
        val left = GroupedEpisodeQueryCacheKey(
            countryCode = CountryCode.FR,
            searchTypes = arrayOf(LangType.SUBTITLES, LangType.VOICE),
            sort = listOf(SortParameter("name", SortParameter.Order.ASC)),
            page = 1,
            limit = 24,
        )
        val right = GroupedEpisodeQueryCacheKey(
            countryCode = CountryCode.FR,
            searchTypes = arrayOf(LangType.SUBTITLES, LangType.VOICE),
            sort = listOf(SortParameter("name", SortParameter.Order.ASC)),
            page = 1,
            limit = 24,
        )

        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
        assertEquals("FR,[SUBTITLES, VOICE],[name,ASC],1,24", left.toString())
    }

    @Test
    fun `countryCode uuid sort pagination key keeps interface inheritance and stable string key`() {
        val key = AnimeQueryCacheKey(
            countryCode = CountryCode.FR,
            uuid = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            name = "One Piece",
            searchTypes = arrayOf(LangType.VOICE),
            sort = listOf(SortParameter("createdAt", SortParameter.Order.DESC)),
            page = 2,
            limit = 12,
        )

        val countryPageableKey: CountryPageableCacheKey = key
        val searchTypesKey: SearchTypesCacheKey = key
        val sortParametersKey: SortParametersCacheKey = key

        assertEquals(2, countryPageableKey.page)
        assertEquals(arrayOf(LangType.VOICE).contentToString(), searchTypesKey.searchTypes.contentToString())
        assertEquals(listOf(SortParameter("createdAt", SortParameter.Order.DESC)), sortParametersKey.sort)
        assertEquals(
            "FR,11111111-1111-1111-1111-111111111111,One Piece,[VOICE],[createdAt,DESC],2,12",
            key.toString()
        )
    }

    @Test
    fun `countryCode local date key compares arrays by content`() {
        val left = WeeklyAnimeQueryCacheKey(
            countryCode = CountryCode.FR,
            uuid = UUID.fromString("22222222-2222-2222-2222-222222222222"),
            weekStartDate = LocalDate.parse("2026-04-24"),
            searchTypes = arrayOf(LangType.SUBTITLES),
        )
        val right = WeeklyAnimeQueryCacheKey(
            countryCode = CountryCode.FR,
            uuid = UUID.fromString("22222222-2222-2222-2222-222222222222"),
            weekStartDate = LocalDate.parse("2026-04-24"),
            searchTypes = arrayOf(LangType.SUBTITLES),
        )
        val different = WeeklyAnimeQueryCacheKey(
            countryCode = CountryCode.FR,
            uuid = UUID.fromString("22222222-2222-2222-2222-222222222222"),
            weekStartDate = LocalDate.parse("2026-04-24"),
            searchTypes = arrayOf(LangType.VOICE),
        )

        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
        assertNotEquals(left, different)
    }
}
