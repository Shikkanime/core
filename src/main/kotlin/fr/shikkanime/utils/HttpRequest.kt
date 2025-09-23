package fr.shikkanime.utils

import com.microsoft.playwright.*
import com.microsoft.playwright.options.Cookie
import fr.shikkanime.entities.enums.CountryCode
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.system.measureTimeMillis

private const val TIMEOUT = 60_000L
private const val BROWSER_TIMEOUT = 15_000L
private val logger = LoggerFactory.getLogger(HttpRequest::class.java)

class HttpRequest(
    val countryCode: CountryCode? = null,
    private val userAgent: String? = null
) : AutoCloseable {
    private var isBrowserInitialized = false
    private var playwright: Playwright? = null
    private var browser: Browser? = null
    private var context: BrowserContext? = null
    private var page: Page? = null

    private fun httpClient() = HttpClient(OkHttp) {
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

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse {
        logger.info("Making request to $url... (GET)")
        val start = System.currentTimeMillis()

        val response = httpClient().use {
            it.get(url) {
                headers.forEach(::header)
            }
        }

        logger.info("Request to $url done in ${System.currentTimeMillis() - start}ms (GET)")
        return response
    }

    suspend fun post(url: String, headers: Map<String, String> = emptyMap(), body: Any? = null): HttpResponse {
        logger.info("Making request to $url... (POST)")
        val start = System.currentTimeMillis()

        val response = httpClient().use {
            if (body is List<*> && body.all { element -> element is PartData }) {
                @Suppress("UNCHECKED_CAST")
                it.submitFormWithBinaryData(url, body as List<PartData>) {
                    headers.forEach(::header)
                }
            } else {
                it.post(url) {
                    headers.forEach(::header)
                    body?.let(::setBody)
                }
            }
        }

        logger.info("Request to $url done in ${System.currentTimeMillis() - start}ms (POST)")
        return response
    }

    private fun initBrowser() {
        if (isBrowserInitialized) {
            return
        }

        playwright = Playwright.create()
        browser = playwright?.firefox()?.launch(BrowserType.LaunchOptions().setHeadless(true))

        context = if (countryCode != null)
            browser?.newContext(
                Browser.NewContextOptions()
                    .setGeolocation(countryCode.latitude, countryCode.longitude)
                    .setPermissions(listOf("geolocation"))
                    .setLocale(countryCode.locale)
                    .setTimezoneId(countryCode.timezone)
                    .apply { this@HttpRequest.userAgent?.let { setUserAgent(it) } }
            ) else browser?.newContext()

        page = context?.newPage()
        page?.setDefaultTimeout(BROWSER_TIMEOUT.toDouble())
        page?.setDefaultNavigationTimeout(BROWSER_TIMEOUT.toDouble())
        isBrowserInitialized = true
    }

    fun getWithBrowser(url: String, selector: String? = null, retryCount: Int = 1): Document {
        initBrowser()
        logger.info("Making request to $url... (BROWSER)")

        val elapsedTime = measureTimeMillis {
            try {
                page?.navigate(url)
                selector?.let { page?.waitForSelector(it) } ?: page?.waitForLoadState()
            } catch (e: Exception) {
                if (retryCount < 3) {
                    logger.info("Retrying...")
                    return getWithBrowser(url, selector, retryCount + 1)
                }
                throw e
            }
        }

        val content = page?.content() ?: throw Exception("Content is null")
        logger.info("Request to $url done in ${elapsedTime}ms (BROWSER)")
        return Jsoup.parse(content)
    }

    fun getCookiesWithBrowser(url: String): Pair<Document, List<Cookie>> {
        initBrowser()
        logger.info("Making request to $url... (BROWSER)")

        val takeMs = measureTimeMillis {
            page?.navigate(url)
            page?.waitForLoadState()
        }

        val cookies = context?.cookies(url) ?: emptyList()
        logger.info("Request to $url done in ${takeMs}ms (BROWSER)")
        return Jsoup.parse(page?.content() ?: throw Exception("Content is null")) to cookies
    }

    override fun close() {
        page?.close()
        context?.close()
        browser?.close()
        playwright?.close()
    }

    companion object {
        fun <T> retry(times: Int, delay: Long = 500, operation: suspend () -> T): T {
            var lastException: Exception? = null

            repeat(times) { attempt ->
                try {
                    return runBlocking { operation() }
                } catch (e: Exception) {
                    lastException = e
                    logger.warning("Attempt $attempt failed: ${e.message}")

                    if (attempt < times - 1) {
                        logger.warning("Retrying in $delay ms...")
                        Thread.sleep(delay)
                    }
                }
            }

            throw (lastException ?: Exception("Failed after $times attempts"))
        }
    }
}