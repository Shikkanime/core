package fr.shikkanime.api

import fr.shikkanime.database.DatabaseManager
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * End-to-end runs against a real embedded server, with real HTTP requests.
 *
 * A mocked test cannot see a content negotiation problem: this suite boots the application exactly
 * like [main] does and speaks HTTP to it, so a GraphQL body that cannot be parsed or a route that
 * never registers fails here, not during a manual run. The tests Ziedelth reviewed were green
 * while the endpoint was unusable - this is the class of lie they must no longer tell.
 */
class HttpIntegrationTest {

    companion object {
        private const val PORT = 18080

        /**
         * Boots the application on [PORT] once: Koin is a global context and the graph must not be
         * started twice.
         */
        @JvmStatic
        @org.junit.jupiter.api.BeforeAll
        fun startServer() {
            DatabaseManager("jdbc:h2:mem:${Uuid.random()};DB_CLOSE_DELAY=-1").apply {
                databaseWrapper.connect()
                databaseWrapper.initializeSchema()
            }

            embeddedServer(CIO, port = PORT) {
                configureApplication()
            }.start(wait = false)

            Thread.sleep(1500)
        }
    }

    private val client: HttpClient = HttpClient.newHttpClient()

    /**
     * A GraphQL query body must be accepted and answered with data.
     */
    @Test
    fun `should answer a graphql query posted as json`() {
        // Given
        val request = HttpRequest.newBuilder(URI("http://localhost:$PORT/graphql"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("""{"query":"{ animes { id } }"}"""))
            .build()

        // When
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // Then
        assertEquals(200, response.statusCode(), response.body())
        assertTrue(response.body().contains("\"data\""), response.body())
    }

    /**
     * The REST endpoint must answer with the stored animes.
     */
    @Test
    fun `should answer the rest endpoint with a json array`() {
        // Given
        val request = HttpRequest.newBuilder(URI("http://localhost:$PORT/animes"))
            .header("Accept", "application/json")
            .GET()
            .build()

        // When
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // Then
        assertEquals(200, response.statusCode(), response.body())
        assertTrue(response.body().startsWith("["), response.body())
    }

    /**
     * A malformed identifier must be refused with a 400, not a 500.
     */
    @Test
    fun `should refuse a malformed identifier with a bad request`() {
        // Given
        val request = HttpRequest.newBuilder(URI("http://localhost:$PORT/animes/not-a-uuid"))
            .header("Accept", "application/json")
            .GET()
            .build()

        // When
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // Then
        assertEquals(400, response.statusCode(), response.body())
    }
}
