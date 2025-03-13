package fr.shikkanime.utils

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class ExtensionsKtTest {
    @Test
    fun isAfterOrEqual() {
        val zda1 = ZonedDateTime.parse("2021-01-01T00:00:00Z").withNano(0).withUTC()
        val zda2 = ZonedDateTime.parse("2021-01-01T00:00:00Z").withNano(0)

        assertTrue(zda1.isAfterOrEqual(zda2))
    }
}