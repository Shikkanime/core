package fr.shikkanime.controllers.api

import com.google.gson.reflect.TypeToken
import fr.shikkanime.dtos.PlatformDto
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.module
import fr.shikkanime.utils.ObjectParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlatformControllerTest : AbstractControllerTest() {
    @Test
    fun getAll() {
        testApplication {
            application {
                module()
            }

            client.get("/api/v1/platforms") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                val platforms = ObjectParser.fromJson(bodyAsText(), object : TypeToken<List<PlatformDto>>() {})
                
                // Verify we get all platforms
                assertEquals(Platform.entries.size, platforms.size)
                
                // Verify platforms are sorted by sortIndex
                val sortedPlatforms = Platform.entries.sortedBy { it.sortIndex }
                platforms.forEachIndexed { index, platformDto ->
                    val expectedPlatform = sortedPlatforms[index]
                    assertEquals(expectedPlatform.name, platformDto.id)
                    assertEquals(expectedPlatform.platformName, platformDto.name)
                    assertEquals(expectedPlatform.url, platformDto.url)
                    assertEquals(expectedPlatform.image, platformDto.image)
                }
                
                // Verify specific platforms exist
                assertTrue(platforms.any { it.name == "Animation Digital Network" })
                assertTrue(platforms.any { it.name == "Crunchyroll" })
                assertTrue(platforms.any { it.name == "Disney+" })
                assertTrue(platforms.any { it.name == "Netflix" })
                assertTrue(platforms.any { it.name == "Prime Video" })
            }
        }
    }
}