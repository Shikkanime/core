package fr.shikkanime.utils

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*


class HttpRequest {
    suspend fun get(url: String): HttpResponse {
        val httpClient = HttpClient(CIO)
        println("Making request to $url... (GET)")
        val start = System.currentTimeMillis()
        val response = httpClient.get(url)
        httpClient.close()
        println("Request to $url done in ${System.currentTimeMillis() - start}ms (GET)")
        return response
    }
}