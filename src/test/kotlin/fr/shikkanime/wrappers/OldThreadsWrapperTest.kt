package fr.shikkanime.wrappers

import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class OldThreadsWrapperTest {
    private val oldThreadsWrapper = OldThreadsWrapper()

    @Test
    fun qeSync() {
        val response = runBlocking { oldThreadsWrapper.qeSync() }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun generateDeviceId() {
        val username = "Hello"
        val password = "World!"
        assertEquals("android-6f36600bd3a8126c", oldThreadsWrapper.generateDeviceId(username, password))
    }

    @Test
    fun encryptPassword() {
        val password = "World!"
        val response = runBlocking { oldThreadsWrapper.encryptPassword(password) }
        assertNotNull(response)
        assertNotNull(response["time"])
        assertNotNull(response["password"])
    }
}