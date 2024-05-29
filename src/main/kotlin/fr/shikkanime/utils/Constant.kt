package fr.shikkanime.utils

import com.google.inject.Guice
import com.google.inject.Injector
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import fr.shikkanime.modules.DefaultModule
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.socialnetworks.AbstractSocialNetwork
import org.reflections.Reflections
import java.io.File
import java.time.ZoneId

object Constant {
    const val NAME = "Shikkanime"
    const val PORT = 37100

    val reflections = Reflections("fr.shikkanime")
    val injector: Injector = Guice.createInjector(DefaultModule())
    val abstractPlatforms = reflections.getSubTypesOf(AbstractPlatform::class.java).map { injector.getInstance(it) }
    val seasons = listOf("WINTER", "SPRING", "SUMMER", "AUTUMN")
    val dataFolder: File
        get() {
            val folder = File("data")
            if (!folder.exists()) folder.mkdirs()
            return folder
        }
    val configFolder: File
        get() {
            val folder = File(dataFolder, "config")
            if (!folder.exists()) folder.mkdirs()
            return folder
        }
    val abstractSocialNetworks =
        reflections.getSubTypesOf(AbstractSocialNetwork::class.java).map { injector.getInstance(it) }
    val utcZoneId: ZoneId = ZoneId.of("UTC")
    val playwright: Playwright = Playwright.create()
    val launchOptions: BrowserType.LaunchOptions = BrowserType.LaunchOptions().setHeadless(true)
    val jwtAudience: String = System.getenv("JWT_AUDIENCE") ?: "jwt-audience"
    val jwtDomain: String = System.getenv("JWT_DOMAIN") ?: "https://jwt-provider-domain/"
    val jwtRealm: String = System.getenv("JWT_REALM") ?: "ktor sample app"
    val jwtSecret: String = System.getenv("JWT_SECRET") ?: "secret"
    val apiUrl: String = System.getenv("API_URL") ?: "http://localhost:$PORT/api"
    val baseUrl: String = System.getenv("BASE_URL") ?: "http://localhost:$PORT"
    val DEFAULT_IMAGE_PREVIEW = "$baseUrl/assets/img/episode_no_image_preview.jpg"
    const val DEFAULT_CACHE_DURATION = 31536000 // 1 year

    init {
        abstractPlatforms.forEach {
            it.loadConfiguration()
        }
    }
}