package fr.shikkanime.wrappers

import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ThreadsWrapperTest {
    private val threadsWrapper = ThreadsWrapper()

    @Test
    fun qeSync() {
        val response = runBlocking { threadsWrapper.qeSync() }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun generateDeviceId() {
        val username = "Hello"
        val password = "World!"
        assertEquals("android-da88e3773489b230", threadsWrapper.generateDeviceId(username, password))
    }

    @Test
    fun encryptPassword() {
        val password = "World!"
        val response = runBlocking { threadsWrapper.encryptPassword(password) }
        assertNotNull(response)
        assertNotNull(response["time"])
        assertNotNull(response["password"])
    }
}