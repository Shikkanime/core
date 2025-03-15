package fr.shikkanime.services

import fr.shikkanime.AbstractTest
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.EpisodeMapping
import fr.shikkanime.entities.EpisodeVariant
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.ImageType
import fr.shikkanime.entities.enums.Platform
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
import java.time.ZonedDateTime
import java.util.*

class ImageServiceTest : AbstractTest() {
    @Test
    fun add() {
        val uuid = UUID.randomUUID()
        ImageService.add(
            uuid,
            ImageType.BANNER,
            "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/8bfa5ecce45d2d497f88f0b1a0f511df.jpe",
            null,
        )
        var i = 0

        while (ImageService[uuid, ImageType.BANNER] == null || ImageService[uuid, ImageType.BANNER]?.bytes?.isEmpty() == true) {
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
    fun addAll() {
        val zonedDateTime = ZonedDateTime.parse("2015-08-22T01:00:00Z")

        val anime = animeService.save(
            Anime(
                countryCode = CountryCode.FR,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                name = "Rent-a-Girlfriend",
                slug = "rent-a-girlfriend",
                image = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1560x2340/catalog/crunchyroll/20fc1f9c50e855bb8bbeefeab10434ff.jpe",
                banner = "https://www.crunchyroll.com/imgsrv/display/thumbnail/1920x1080/catalog/crunchyroll/3831353540b0f1547c202f2df446cf2c.jpe",
            )
        )

        val episodeMapping = episodeMappingService.save(
            EpisodeMapping(
                anime = anime,
                releaseDateTime = zonedDateTime,
                lastReleaseDateTime = zonedDateTime,
                lastUpdateDateTime = zonedDateTime,
                season = 1,
                episodeType = EpisodeType.EPISODE,
                number = 1,
                image = "test.jpg"
            )
        )

        episodeVariantService.save(
            EpisodeVariant(
                mapping = episodeMapping,
                releaseDateTime = zonedDateTime,
                platform = Platform.CRUN,
                audioLocale = "fr-FR",
                identifier = "FR-CRUN-GZ7UV8KWZ-FR-FR",
                url = "https://www.crunchyroll.com/fr/watch/GZ7UV8KWZ/rent-a-girlfriend"
            )
        )

        ImageService.addAll(bypass = true)

        var i = 0

        while (ImageService[anime.uuid!!, ImageType.BANNER] == null || ImageService[anime.uuid, ImageType.BANNER]?.bytes?.isEmpty() == true) {
            runBlocking { delay(1000) }

            if (i++ > 10) {
                throw Exception("Image not found")
            }
        }

        testApplication {
            application {
                module()
            }

            client.get("/api/v1/attachments?uuid=${anime.uuid}&type=${ImageType.BANNER}").apply {
                assertEquals(HttpStatusCode.OK, status)
                val byteArrayOutputStream = ByteArrayOutputStream()
                runBlocking { bodyAsChannel().copyTo(byteArrayOutputStream) }
                val image = byteArrayOutputStream.toByteArray()
                assertTrue(image.isNotEmpty())
            }
        }
    }
}