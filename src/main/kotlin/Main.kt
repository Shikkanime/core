package fr.shikkanime

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    explicitNulls = false
                }
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
        }
    }

suspend fun main() {
    val locale = "fr-FR"

    createHttpClient().use { httpClient ->
        val primeVideoWrapper = PrimeVideoWrapper(httpClient)
        val primeVideoProvider = PrimeVideoProvider(primeVideoWrapper)

        primeVideoProvider.getEpisodes(locale, "0QA3P8T387P0WAV0KXUYBWDDYR").forEach { println(it) }
    }
}