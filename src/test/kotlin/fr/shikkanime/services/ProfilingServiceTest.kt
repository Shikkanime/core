package fr.shikkanime.services

import com.google.inject.Inject
import fr.shikkanime.AbstractTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfilingServiceTest : AbstractTest() {
    @Inject
    private lateinit var profilingService: ProfilingService

    @Test
    fun testJfrRecording() {
        profilingService.startGlobalRecording()
        profilingService.dumpAndRestart()
        val files = profilingService.getJfrFiles()
        assertTrue(files.any { it.name.startsWith("profile-") && it.name.endsWith(".jfr") })
    }
}
