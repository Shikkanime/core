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
        ImageService.add(
            uuid,
            ImageService.Type.IMAGE,
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/8bfa5ecce45d2d497f88f0b1a0f511df.jpe",
            128,
            128
        )
        var i = 0

        while (ImageService[uuid, ImageService.Type.IMAGE] == null || ImageService[uuid, ImageService.Type.IMAGE]?.bytes?.isEmpty() == true) {
            runBlocking { delay(1000) }

            if (i++ > 10) {
                throw Exception("Image not found")
            }
        }

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

    @Test
    fun getLongTimeoutImage() {
        ImageService.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/fcaab22f-cc08-463e-8cc8-bf4367fb1027/compose")
        ImageService.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/1d043ddd-5295-40bb-8e65-0aa3ec34b301/compose")
        ImageService.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/bf85d6d3-d182-4570-adff-1261b843c864/compose")

        ImageService.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/6b27e3d6-74e5-4ea1-a0f1-79ca316dd9b0/compose")
        ImageService.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/3cfa2215-a96d-4064-a2bd-1ebd5dbe1eec/compose")
        ImageService.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/707bc5c2-f9ef-4eeb-8c3e-3967e54a62bf/compose")
        ImageService.getLongTimeoutImage("https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/6b27e3d6-74e5-4ea1-a0f1-79ca316dd9b0/compose")
    }

    @Test
    fun getLongTimeoutImageMultiThreads() {
        val images = listOf(
            "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/6b27e3d6-74e5-4ea1-a0f1-79ca316dd9b0/compose",
            "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/3cfa2215-a96d-4064-a2bd-1ebd5dbe1eec/compose",
            "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/707bc5c2-f9ef-4eeb-8c3e-3967e54a62bf/compose",
            "https://disney.images.edge.bamgrid.com/ripcut-delivery/v2/variant/disney/6b27e3d6-74e5-4ea1-a0f1-79ca316dd9b0/compose"
        )

        images.parallelStream().forEach {
            ImageService.getLongTimeoutImage(it)
        }
    }
}