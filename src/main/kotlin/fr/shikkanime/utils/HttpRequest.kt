package fr.shikkanime.utils

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.system.measureTimeMillis

private const val TIMEOUT = 60_000L
private val logger = LoggerFactory.getLogger(HttpRequest::class.java)

class HttpRequest : AutoCloseable {
    private var isBrowserInitialized = false
    private var browser: Browser? = null
    private var page: Page? = null

    private fun httpClient(): HttpClient {
        val httpClient = HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT
                connectTimeoutMillis = TIMEOUT
                socketTimeoutMillis = TIMEOUT
            }
            engine {
                config {
                    followRedirects(true)
                }
            }
        }
        return httpClient
    }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse {
        val httpClient = httpClient()

        logger.info("Making request to $url... (GET)")
        val start = System.currentTimeMillis()
        val response = httpClient.get(url) {
            headers.forEach { (key, value) ->
                header(key, value)
            }
        }

        httpClient.close()
        logger.info("Request to $url done in ${System.currentTimeMillis() - start}ms (GET)")
        return response
    }

    suspend fun post(url: String, headers: Map<String, String> = emptyMap(), body: String): HttpResponse {
        val httpClient = httpClient()
        logger.info("Making request to $url... (POST)")
        val start = System.currentTimeMillis()
        val response = httpClient.post(url) {
            headers.forEach { (key, value) ->
                header(key, value)
            }

            setBody(body)
        }
        httpClient.close()
        logger.info("Request to $url done in ${System.currentTimeMillis() - start}ms (POST)")
        return response
    }

    private fun initBrowser() {
        if (isBrowserInitialized) {
            return
        }

        browser = Constant.playwright.firefox().launch(Constant.launchOptions)
        page = browser?.newPage()
        page?.setDefaultTimeout(TIMEOUT.toDouble())
        page?.setDefaultNavigationTimeout(TIMEOUT.toDouble())
        isBrowserInitialized = true
    }

    fun getBrowser(url: String, selector: String? = null, retry: Int = 3): Document {
        initBrowser()
        logger.info("Making request to $url... (BROWSER)")

        val takeMs = measureTimeMillis {
            try {
                page?.navigate(url)

                if (selector != null) {
                    page?.waitForSelector(selector)
                } else {
                    page?.waitForLoadState()
                }
            } catch (e: Exception) {
                if (retry > 0) {
                    logger.info("Retrying...")
                    return getBrowser(url, selector, retry - 1)
                }

                throw e
            }
        }

        val content = page?.content()
        logger.info("Request to $url done in ${takeMs}ms (BROWSER)")
        return Jsoup.parse(content ?: throw Exception("Content is null"))
    }

    val lastPageUrl: String?
        get() = page?.url()

    override fun close() {
        page?.close()
        browser?.close()
    }
}