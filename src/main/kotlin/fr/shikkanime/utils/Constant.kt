package fr.shikkanime.utils

import com.google.inject.Guice
import com.google.inject.Injector
import fr.shikkanime.modules.DefaultModule
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.socialnetworks.AbstractSocialNetwork
import org.reflections.Reflections
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.io.path.createTempDirectory

object Constant {
    const val NAME = "Shikkanime"
    const val PORT = 37100

    val reflections = Reflections("fr.shikkanime")
    val injector: Injector = Guice.createInjector(DefaultModule())
    val abstractPlatforms = reflections.getSubTypesOf(AbstractPlatform::class.java).map { injector.getInstance(it) }

    val isTest: Boolean
        get() = (System.getenv("IS_TEST")?.toBoolean() ?: System.getProperty("isTest")?.toBoolean()) == true

    private val tmpDirectory: File = createTempDirectory("shikkanime").toFile()
    val dataFolder: File
        get() {
            val folder = if (isTest) tmpDirectory else File("data")
            if (!folder.exists()) folder.mkdirs()
            return folder
        }
    val configFolder: File
        get() {
            val folder = File(dataFolder, "config")
            if (!folder.exists()) folder.mkdirs()
            return folder
        }
    val imagesFolder: File
        get() {
            val folder = File(dataFolder, "images")
            if (!folder.exists()) folder.mkdirs()
            return folder
        }

    val abstractSocialNetworks = reflections.getSubTypesOf(AbstractSocialNetwork::class.java).map { injector.getInstance(it) }
    val utcZoneId: ZoneId = ZoneId.of("UTC")
    val jwtAudience: String = System.getenv("JWT_AUDIENCE") ?: "jwt-audience"
    val jwtDomain: String = System.getenv("JWT_DOMAIN") ?: "https://jwt-provider-domain/"
    val jwtRealm: String = System.getenv("JWT_REALM") ?: "ktor sample app"
    val jwtSecret: String = System.getenv("JWT_SECRET") ?: "secret"
    val apiUrl: String = System.getenv("API_URL") ?: "http://localhost:$PORT/api"
    val baseUrl: String = System.getenv("BASE_URL") ?: "http://localhost:$PORT"
    val DEFAULT_IMAGE_PREVIEW = "$baseUrl/assets/img/episode_no_image_preview.jpg"
    const val DEFAULT_CACHE_DURATION = 31536000 // 1 year
    const val MAX_DESCRIPTION_LENGTH = 1_000
    val oldLastUpdateDateTime: ZonedDateTime = ZonedDateTime.parse("2000-01-01T00:00:00Z")
    val valkeyHost: String = System.getenv("VALKEY_HOST") ?: "localhost"
    val valkeyPort: Int = (System.getenv("VALKEY_PORT")?.toIntOrNull() ?: 6379)

    init {
        abstractPlatforms.forEach {
            it.loadConfiguration()
        }
    }
}