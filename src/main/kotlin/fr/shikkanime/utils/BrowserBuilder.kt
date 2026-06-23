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
    private enum class BrowserEngine {
        CHROMIUM,
        FIREFOX
    }

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
        val userDataDir = Files.createTempDirectory("chrome-profile")
        val browserEngine = getBrowserEngine()
        val executablePath = if (browserEngine == BrowserEngine.CHROMIUM) findChromiumExecutablePath() else null
        val browserArgs = if (browserEngine == BrowserEngine.CHROMIUM) buildChromiumArgs() else emptyList()
        logger.info(
            "Browser runtime: engine=$browserEngine, os.arch=${System.getProperty("os.arch")}, DISPLAY=${System.getenv("DISPLAY")}, " +
                "PLAYWRIGHT_BROWSERS_PATH=${System.getenv("PLAYWRIGHT_BROWSERS_PATH")}, MOZ_GMP_PATH=${System.getenv("MOZ_GMP_PATH")}, " +
                "userDataDir=$userDataDir, executablePath=${executablePath?.absolutePath ?: "<playwright-default>"}, args=${browserArgs.joinToString(" ")}"
        )
        val launchOptions = BrowserType.LaunchPersistentContextOptions()
            .setHeadless(false)
            .setEnv(System.getenv())
        if (browserEngine == BrowserEngine.CHROMIUM) {
            launchOptions
                .setIgnoreDefaultArgs(listOf("--disable-component-update"))
                .setArgs(browserArgs)
        } else {
            launchOptions.setFirefoxUserPrefs(firefoxWidevinePrefs())
        }
        executablePath?.let { launchOptions.setExecutablePath(it.toPath()) }

        val browserType = when (browserEngine) {
            BrowserEngine.CHROMIUM -> playwright.chromium()
            BrowserEngine.FIREFOX -> playwright.firefox()
        }
        val context = browserType.launchPersistentContext(userDataDir, launchOptions)

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

    private fun getBrowserEngine(): BrowserEngine {
        return when (System.getenv("SHIKKANIME_BROWSER")?.lowercase()) {
            "firefox" -> BrowserEngine.FIREFOX
            else -> BrowserEngine.CHROMIUM
        }
    }

    private fun findChromiumExecutablePath(): File? {
        val configuredPath = System.getenv("SHIKKANIME_CHROMIUM_EXECUTABLE_PATH")
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.isFile && it.canExecute() }

        if (configuredPath != null) {
            return configuredPath
        }

        val browsersPath = File(System.getenv("PLAYWRIGHT_BROWSERS_PATH") ?: "/opt/playwright")
        return browsersPath.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("chromium-") && it.name != "chromium_headless_shell" }
            ?.mapNotNull { File(it, "chrome-linux/chrome").takeIf { chrome -> chrome.isFile && chrome.canExecute() } }
            ?.sortedBy { it.absolutePath }
            ?.firstOrNull()
    }

    private fun firefoxWidevinePrefs() = mapOf<String, Any>(
        "media.eme.enabled" to true,
        "media.eme.encrypted-media-encryption-scheme.enabled" to true,
        "media.gmp-widevinecdm.enabled" to true,
        "media.gmp-widevinecdm.visible" to true,
        "media.gmp-widevinecdm.autoupdate" to false,
        "media.gmp-widevinecdm.version" to "system-installed",
        "media.gmp-manager.updateEnabled" to false,
        "media.gmp-provider.enabled" to true
    )

    private fun buildChromiumArgs(): List<String> {
        val args = mutableListOf(
            "--autoplay-policy=no-user-gesture-required",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--enable-logging=stderr",
            "--vmodule=*widevine*=2,*cdm*=2,*media*=1"
        )

        val widevineLibrary = listOf(
            File("/opt/WidevineCdm/gmp-widevinecdm/latest/libwidevinecdm.so"),
            File("/var/lib/widevine/libwidevinecdm.so")
        ).firstOrNull { it.isFile }
        if (widevineLibrary != null) {
            args.add("--widevine-cdm-path=${widevineLibrary.absolutePath}")
            listOf(
                File("/opt/WidevineCdm/manifest.json"),
                File("/var/lib/widevine/manifest.json")
            ).firstOrNull { it.isFile }?.readText()
                ?.let { Regex("\"version\"\\s*:\\s*\"([^\"]+)\"").find(it)?.groupValues?.getOrNull(1) }
                ?.let { args.add("--widevine-cdm-version=$it") }
        }

        return args
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
                            const details = {
                                userAgent: navigator.userAgent,
                                platform: navigator.platform,
                                userAgentDataPlatform: navigator.userAgentData?.platform ?? null,
                                hasRequestMediaKeySystemAccess: !!navigator.requestMediaKeySystemAccess
                            };
                            try {
                                if (!navigator.requestMediaKeySystemAccess) {
                                    details.result = 'API_NOT_FOUND';
                                    return JSON.stringify(details);
                                }
                                const access = await navigator.requestMediaKeySystemAccess('com.widevine.alpha', [{
                                    initDataTypes: ['cenc', 'keyids', 'webm'],
                                    distinctiveIdentifier: 'optional',
                                    persistentState: 'optional',
                                    sessionTypes: ['temporary'],
                                    videoCapabilities: [
                                        { contentType: 'video/mp4; codecs="avc1.42E01E"' },
                                        { contentType: 'video/webm; codecs="vp9"' }
                                    ],
                                    audioCapabilities: [
                                        { contentType: 'audio/mp4; codecs="mp4a.40.2"' },
                                        { contentType: 'audio/webm; codecs="opus"' }
                                    ]
                                }]);
                                details.result = 'SUPPORTED';
                                details.keySystem = access.keySystem;
                                details.configuration = access.getConfiguration();
                                return JSON.stringify(details);
                            } catch (e) {
                                details.result = 'ERROR';
                                details.errorName = e.name;
                                details.errorMessage = e.message;
                                return JSON.stringify(details);
                            }
                        }
                    """.trimIndent())
                    .build()
                val response = results.firstOrNull()?.second
                val result = response?.contains("\"result\":\"SUPPORTED\"") == true
                if (result) {
                    logger.info("Widevine DRM is supported! Response: $response")
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