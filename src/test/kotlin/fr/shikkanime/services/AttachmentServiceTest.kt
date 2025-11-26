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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayOutputStream
import java.util.*

class AttachmentServiceTest : AbstractTest() {
    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/8bfa5ecce45d2d497f88f0b1a0f511df.jpe",
            "https://media.animationdigitalnetwork.com/images/show/f06b0d28-9228-492c-ab63-7817ab48d57c/affiche.width=1560,height=2340,quality=100",
            "https://media.animationdigitalnetwork.com/images/show/f06b0d28-9228-492c-ab63-7817ab48d57c/license.width=1920,height=1080,quality=100",
            "https://media.animationdigitalnetwork.com/images/show/f06b0d28-9228-492c-ab63-7817ab48d57c/carousel169.width=1920,height=1080,quality=100",
            "https://media.animationdigitalnetwork.com/images/show/7ae5e0e2-277a-4fd8-bbd3-dceb58ce43df/affiche.width=1560,height=2340,quality=100",
            "https://media.animationdigitalnetwork.com/images/show/7ae5e0e2-277a-4fd8-bbd3-dceb58ce43df/license.width=1920,height=1080,quality=100",
            "https://media.animationdigitalnetwork.com/images/show/7ae5e0e2-277a-4fd8-bbd3-dceb58ce43df/carousel169.width=1920,height=1080,quality=100",
        ]
    )
    fun webpConversion(url: String) {
        val uuid = UUID.randomUUID()

        val attachment = attachmentService.createAttachmentOrMarkAsActive(
            uuid,
            ImageType.BANNER,
            url = url,
        )

        val file = attachmentService.getFile(attachment)
        var i = 0

        while (!file.exists() || file.length() <= 0) {
            runBlocking { delay(1000) }

            if (i++ > 10) {
                throw Exception("Image not found for url $url")
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