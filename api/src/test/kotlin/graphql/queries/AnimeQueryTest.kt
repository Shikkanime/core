package fr.shikkanime.api.graphql.queries

import fr.shikkanime.database.builders.impl.AnimeMockKBuilder
import fr.shikkanime.database.services.AnimeService
import fr.shikkanime.graphql.configureGraphQL
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@ExtendWith(MockKExtension::class)
@DisplayName("tests for the anime graphql queries")
class AnimeQueryTest {
    @MockK private lateinit var animeService: AnimeService

    private fun Application.registerGraphQL() {
        configureGraphQL(
            schemaPackages = listOf("fr.shikkanime.api.graphql.queries", "fr.shikkanime.api.dtos"),
            schemaQueries = listOf(AnimeQuery(animeService))
        )
    }

    private val anime = AnimeMockKBuilder {
        id = Uuid.random()
        images = emptyList()
        localizations = emptyList()
        platforms = emptyList()
    }.build()

    @Nested
    @DisplayName("tests for the 'animes' query")
    inner class AnimesTests {
        @Test
        fun `should return the stored animes`() =
            testApplication {
                // Given
                every { animeService.findAll() } returns listOf(anime)
                application { registerGraphQL() }

                // When
                val response = client.post("/graphql") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"query":"{ animes { id } }"}""")
                }

                // Then
                assertEquals(HttpStatusCode.OK, response.status)
                assertTrue(response.bodyAsText().contains(anime.id.toString()))
            }
    }
}
