package fr.shikkanime.services

import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.enums.ImageType
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

class AttachmentServiceTest : AbstractTest() {
    @Test
    fun createAttachmentOrMarkAsActive() {
        val uuid = UUID.randomUUID()

        val attachment = attachmentService.createAttachmentOrMarkAsActive(
            uuid,
            ImageType.BANNER,
            url = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/8bfa5ecce45d2d497f88f0b1a0f511df.jpe",
        )

        val file = attachmentService.getFile(attachment)
        var i = 0

        while (!file.exists() || file.length() <= 0) {
            runBlocking { delay(1000) }

            if (i++ > 10) {
                throw Exception("Image not found")
            }
        }

        testApplication {
            application {
                module()
            }

            client.get("/api/v1/attachments?uuid=${uuid}&type=${ImageType.BANNER}").apply {
                assertEquals(HttpStatusCode.OK, status)
                val byteArrayOutputStream = ByteArrayOutputStream()
                runBlocking { bodyAsChannel().copyTo(byteArrayOutputStream) }
                val image = byteArrayOutputStream.toByteArray()
                assertTrue(image.isNotEmpty())
            }
        }
    }
}