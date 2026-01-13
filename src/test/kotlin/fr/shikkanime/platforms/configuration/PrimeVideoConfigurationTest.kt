package fr.shikkanime.platforms.configuration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.ZonedDateTime
import java.util.*
import java.util.stream.Stream

class PrimeVideoConfigurationTest {
    companion object {
        @JvmStatic
        fun canBeFetchProvider(): Stream<Arguments> = Stream.of(
            // Monday 16:31
            Arguments.of(1, "16:31", "2025-01-13T16:30:59Z", false),
            Arguments.of(1, "16:31", "2025-01-13T16:31:00Z", true),
            Arguments.of(1, "16:31", "2025-01-13T16:31:01Z", true),
            // Friday 16:31
            Arguments.of(5, "16:31", "2025-01-10T16:30:59Z", false),
            Arguments.of(5, "16:31", "2025-01-10T16:31:00Z", true),
            // All days (0), 16:31
            Arguments.of(0, "16:31", "2025-01-10T16:30:59Z", false),
            Arguments.of(0, "16:31", "2025-01-10T16:31:00Z", true),
            // No release time (always true if day matches)
            Arguments.of(5, "", "2025-01-10T00:00:00Z", true),
            Arguments.of(5, "", "2025-01-11T00:00:00Z", false),
            // Boundary cases
            Arguments.of(1, "00:00", "2025-01-13T00:00:00Z", true),
            Arguments.of(1, "23:59", "2025-01-13T23:58:59Z", false),
            Arguments.of(1, "23:59", "2025-01-13T23:59:00Z", true)
        )
    }

    @ParameterizedTest
    @MethodSource("canBeFetchProvider")
    fun testCanBeFetch(releaseDay: Int, releaseTime: String, zonedDateTime: String, expected: Boolean) {
        val currentDefault = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            val simulcast = PrimeVideoConfiguration.PrimeVideoSimulcast(
                releaseTime = releaseTime
            )
            simulcast.releaseDay = releaseDay
            assertEquals(expected, simulcast.canBeFetch(ZonedDateTime.parse(zonedDateTime)), "Failed for $releaseDay $releaseTime at $zonedDateTime")
        } finally {
            TimeZone.setDefault(currentDefault)
        }
    }

    @Test
    fun testCanBeFetchWithLocalTimezone() {
        val currentDefault = TimeZone.getDefault()
        // Set to Paris (UTC+1)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"))

        try {
            val simulcast = PrimeVideoConfiguration.PrimeVideoSimulcast(
                releaseTime = "16:31"
            )
            simulcast.releaseDay = 5 // Friday

            // Friday 15:30 UTC = 16:30 Paris -> False
            assertFalse(simulcast.canBeFetch(ZonedDateTime.parse("2025-01-10T15:30:00Z")))
            // Friday 15:31 UTC = 16:31 Paris -> True
            assertTrue(simulcast.canBeFetch(ZonedDateTime.parse("2025-01-10T15:31:00Z")))

            // Sunday 23:31 UTC = Monday 00:31 Paris
            // releaseDay is Monday (1)
            simulcast.releaseDay = 1
            // Monday 00:31 Paris is after 16:31? NO!
            // Sunday 23:31 UTC is after 16:31 numerically, but it's Sunday in UTC and Monday in Paris.
            // With the fix, this should now correctly return FALSE.
            assertFalse(simulcast.canBeFetch(ZonedDateTime.parse("2025-01-12T23:31:00Z")))
        } finally {
            TimeZone.setDefault(currentDefault)
        }
    }
}
