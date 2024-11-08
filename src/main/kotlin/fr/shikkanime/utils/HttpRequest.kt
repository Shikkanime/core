package fr.shikkanime.utils

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import fr.shikkanime.entities.enums.CountryCode
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.system.measureTimeMillis

private const val TIMEOUT = 60_000L
private const val BROWSER_TIMEOUT = 15_000L
private val logger = LoggerFactory.getLogger(HttpRequest::class.java)

class HttpRequest(
    val countryCode: CountryCode? = null
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

    suspend fun post(url: String, headers: Map<String, String> = emptyMap(), body: Any? = null): HttpResponse {
        val httpClient = httpClient()
        logger.info("Making request to $url... (POST)")
        val start = System.currentTimeMillis()
        val response = httpClient.post(url) {
            headers.forEach { (key, value) ->
                header(key, value)
            }

            body?.let { setBody(it) }
        }
        httpClient.close()
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
            ) else
            browser?.newContext()

        page = context?.newPage()
        page?.setDefaultTimeout(BROWSER_TIMEOUT.toDouble())
        page?.setDefaultNavigationTimeout(BROWSER_TIMEOUT.toDouble())
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

    override fun close() {
        page?.close()
        context?.close()
        browser?.close()
        playwright?.close()
    }

    companion object {
        fun <T> retry(times: Int, delay: Long = 500, operation: suspend () -> T): T {
            repeat(times) { attempt ->
                try {
                    return runBlocking { operation() }
                } catch (e: Exception) {
                    logger.warning("Attempt $attempt failed: ${e.message}")

                    if (attempt < times - 1) {
                        logger.warning("Retrying in $delay ms...")
                        Thread.sleep(delay)
                    }
                }
            }

            throw Exception("Failed after $times attempts")
        }
    }
}