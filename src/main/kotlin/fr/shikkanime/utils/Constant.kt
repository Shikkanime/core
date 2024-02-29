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
    val reflections = Reflections("fr.shikkanime")
    val injector: Injector = Guice.createInjector(DefaultModule())
    val abstractPlatforms = reflections.getSubTypesOf(AbstractPlatform::class.java).map { injector.getInstance(it) }
    val seasons = listOf("WINTER", "SPRING", "SUMMER", "AUTUMN")
    val dataFolder: File
        get() {
            val dataFolder = File("data")

            if (!dataFolder.exists()) {
                dataFolder.mkdirs()
            }

            return dataFolder
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

    const val BASE_URL = "https://www.shikkanime.fr"
    const val DEFAULT_IMAGE_PREVIEW = "$BASE_URL/assets/img/episode_no_image_preview.jpg"

    init {
        abstractPlatforms.forEach {
            it.loadConfiguration()
        }
    }
}