package fr.shikkanime.utils

import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.services.caches.ConfigCacheService
import java.io.File
import java.nio.file.Files
import java.util.*

class BrowserBuilder {
    private class BrowserCloseable(
        private val playwright: Playwright,
        private val context: BrowserContext,
        val page: Page
    ) : AutoCloseable {
        operator fun component1() = playwright
        operator fun component2() = context
        operator fun component3() = page

        override fun close() {
            page.close()
            context.close()
            playwright.close()
        }
    }

    data class Cookie(
        val name: String,
        val value: String,
        val domain: String
    )

    private val configCacheService: ConfigCacheService by lazy { Constant.injector.getInstance(ConfigCacheService::class.java) }
    private val cookies = mutableListOf<Cookie>()
    private val urls = mutableSetOf<String>()
    private var waitFunction: String? = null
    private lateinit var evaluateFunction: String

    fun addCookie(cookie: Cookie): BrowserBuilder {
        cookies.add(cookie)
        return this
    }

    fun setUrls(vararg urls: String): BrowserBuilder {
        this.urls.addAll(urls)
        return this
    }

    fun setWaitFunction(waitFunction: String): BrowserBuilder {
        this.waitFunction = waitFunction
        return this
    }

    fun setEvaluateFunction(evaluateFunction: String): BrowserBuilder {
        this.evaluateFunction = evaluateFunction
        return this
    }

    suspend fun build(): List<Pair<String, String>> {
        require(urls.isNotEmpty()) { "At least one url must be provided" }

        logger.info("Launching browser with ${cookies.size} cookies and ${urls.size} URLs...")
        val playwright = Playwright.create()
        val context = playwright.chromium()
            .launchPersistentContext(
                Files.createTempDirectory("chrome-profile"),
                BrowserType.LaunchPersistentContextOptions()
                    .setHeadless(false)
                    .setIgnoreDefaultArgs(listOf("--disable-component-update"))
                    .setArgs(listOf(
                        "--autoplay-policy=no-user-gesture-required",
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-gpu"
                    ))
            )

        logger.info("Setting cookies...")
        context.clearCookies()
        context.addCookies(
            cookies.map {
                com.microsoft.playwright.options.Cookie(it.name, it.value)
                    .setDomain(it.domain)
                    .setPath("/")
                    .setSecure(true)
                    .setHttpOnly(true)
            }
        )

        val result = mutableListOf<Pair<String, String>>()

        BrowserCloseable(
            playwright,
            context,
            context.newPage()
        ).use { (_, _, page) ->
            urls.forEach { url ->
                for (i in 1..3) {
                    try {
                        logger.info("Fetching $url... (try $i/3)")
                        page.navigate(url)
                        waitFunction?.let {
                            page.waitForFunction(
                                it,
                                null,
                                Page.WaitForFunctionOptions()
                                    .setPollingInterval(1000.0)
                                    .setTimeout(configCacheService.getValueAsInt(ConfigPropertyKey.BROWSER_WAIT_TIMEOUT, 10_000).toDouble())
                            )
                        } ?: page.waitForLoadState()

                        result.add(url to page.evaluate(evaluateFunction) as String)
                        break
                    } catch (e: Exception) {
                        if (configCacheService.getValueAsBoolean(ConfigPropertyKey.SAVE_BROWSER_SCREENSHOT_ON_ERROR))
                            File(Constant.browserScreenshotFolder, "${UUID.randomUUID()}.png").writeBytes(page.screenshot())
                        logger.warning("Error while fetching $url: ${e.message} (try $i)")
                    }
                }
            }
        }

        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BrowserBuilder::class.java)

        suspend fun checkWidevine(): Boolean {
            logger.info("Checking Widevine DRM support...")
            return try {
                val results = BrowserBuilder()
                    .setUrls("https://example.com")
                    .setEvaluateFunction("""
                        async () => {
                            try {
                                if (!navigator.requestMediaKeySystemAccess) {
                                    return 'API_NOT_FOUND';
                                }
                                const access = await navigator.requestMediaKeySystemAccess('com.widevine.alpha', [{
                                    initDataTypes: ['keyids', 'webm'],
                                    videoCapabilities: [{ contentType: 'video/webm; codecs="vp9"' }]
                                }]);
                                return 'true';
                            } catch (e) {
                                return e.name + ': ' + e.message;
                            }
                        }
                    """.trimIndent())
                    .build()
                val response = results.firstOrNull()?.second
                val result = response == "true"
                if (result) {
                    logger.info("Widevine DRM is supported!")
                } else {
                    logger.severe("Widevine DRM is NOT supported! Response: $response")
                }
                result
            } catch (e: Exception) {
                logger.severe("Failed to check Widevine DRM support: ${e.message}")
                false
            }
        }
    }
}