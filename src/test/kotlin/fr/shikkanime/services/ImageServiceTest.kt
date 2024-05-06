package fr.shikkanime.services

import fr.shikkanime.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.*

class ImageServiceTest {

    @Test
    fun add() {
        val uuid = UUID.randomUUID()
        ImageService.add(uuid, ImageService.Type.IMAGE, "https://www.shikkanime.fr/assets/img/dark_logo.png", 128, 128)
        runBlocking { delay(1000) }

        testApplication {
            application {
                module()
            }

            client.get("/api/v1/attachments?uuid=${uuid}").apply {
                assertEquals(HttpStatusCode.OK, status)
                val byteArrayOutputStream = ByteArrayOutputStream()
                runBlocking { bodyAsChannel().copyTo(byteArrayOutputStream) }
                val image = byteArrayOutputStream.toByteArray()
                assertTrue(image.isNotEmpty())
            }
        }
    }
}