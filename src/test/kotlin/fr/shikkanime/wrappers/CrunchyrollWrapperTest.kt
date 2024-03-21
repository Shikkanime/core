package fr.shikkanime.wrappers

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.utils.ObjectParser.getAsString
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private const val OLD_TOKEN =
    "eyJhbGciOiJSUzI1NiIsImtpZCI6IkFDNG5YM0JIaW5hbFRoV0pHajY2aEEiLCJ0eXAiOiJKV1QifQ.eyJhbm9ueW1vdXNfaWQiOiIiLCJjbGllbnRfaWQiOiJjcl93ZWIiLCJjbGllbnRfdGFnIjoiKiIsImNvdW50cnkiOiJGUiIsImV4cCI6MTcwNTk0NTA0NSwianRpIjoiZDNjMDU1M2QtOGU2MC00ZTc2LWIyZDgtMmNlYjY4NDcwNzlmIiwibWF0dXJpdHkiOiJNMiIsIm9hdXRoX3Njb3BlcyI6ImFjY291bnQgY29udGVudCBvZmZsaW5lX2FjY2VzcyIsInN0YXR1cyI6IkFOT05ZTU9VUyIsInRudCI6ImNyIn0.QisvFs4k0_TdQHSOQsOJjprM9Vu4DYUcmLlRW-S51iGUgY2oiZTjwNFvNLevl4JKPFqP0zCkW7UB45nOsxnKrLNc7XZKC_Z0soJoIKEXJomI_A_wlutX9TO2a_GIZ4T1ElZpK5PocpcA7ZihDt2_M84IZD0G0o0EBE1lVo56YjcEvDzWCHwvA6XJu14ZO8MIpiyQIMeKt8atFVQJTkULFhwqH-umujsJK_dclwAYA5jgq-q-lFw5q76ZkJ7BpAxnmQqQuxVD3fpmOT2QWVsnzIqBFVerTTO4QWTX1_DvtARv31HD_6wyq7-Xhx-IzQWUm5JbxsNBtYLL614G5rQEMw"

class CrunchyrollWrapperTest {
    private val locale = CountryCode.FR.locale

    @Test
    fun getAnonymousAccessToken() {
        assertNotNull(token)
    }

    @Test
    fun getCMS() {
        assertNotNull(cms)
    }

    @Test
    fun getCMSError() {
        assertThrows<Exception> {
            runBlocking { CrunchyrollWrapper.getCMS(OLD_TOKEN) }
        }
    }

    @Test
    fun getBrowse() {
        val newlyAdded = runBlocking { CrunchyrollWrapper.getBrowse(locale, token!!) }
        assertNotNull(newlyAdded)
        assertEquals(25, newlyAdded.size)
    }

    @Test
    fun getBrowseError() {
        assertThrows<Exception> {
            runBlocking { CrunchyrollWrapper.getBrowse(locale, OLD_TOKEN) }
        }
    }

    @Test
    fun getObject() {
        val `object` = runBlocking { CrunchyrollWrapper.getObject(locale, token!!, cms!!, "G9DUEM48Z") }
        assertNotNull(`object`)
    }

    @Test
    fun getSimulcasts() {
        val simulcasts = runBlocking { CrunchyrollWrapper.getSimulcasts(locale, token!!) }
        assertEquals(true, simulcasts.isNotEmpty())
    }

    @Test
    fun getSimulcastsError() {
        assertThrows<Exception> {
            runBlocking { CrunchyrollWrapper.getSimulcasts(locale, OLD_TOKEN) }
        }
    }

    @Test
    fun getWinter2024Series() {
        val series = runBlocking {
            CrunchyrollWrapper.getBrowse(
                locale,
                token!!,
                sortBy = CrunchyrollWrapper.SortType.POPULARITY,
                type = CrunchyrollWrapper.MediaType.SERIES,
                size = 200,
                simulcast = "winter-2024"
            )
        }
        assertEquals(true, series.isNotEmpty())

        series.forEach {
            println(it.getAsString("title"))
        }
    }

    companion object {
        private var token: String? = null
        private var cms: CrunchyrollWrapper.CMS? = null

        @JvmStatic
        @BeforeAll
        fun setUp() {
            token = runBlocking { CrunchyrollWrapper.getAnonymousAccessToken() }
            cms = runBlocking { CrunchyrollWrapper.getCMS(token!!) }
        }
    }
}