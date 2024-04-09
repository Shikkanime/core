package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import com.microsoft.playwright.Page
import com.microsoft.playwright.junit.UsePlaywright
import fr.shikkanime.converters.AbstractConverter
import fr.shikkanime.dtos.AnimeDto
import fr.shikkanime.entities.Anime
import fr.shikkanime.entities.Episode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.initAll
import fr.shikkanime.services.AnimeService
import fr.shikkanime.services.EpisodeService
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.JobManager
import fr.shikkanime.utils.ObjectParser
import io.ktor.server.engine.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.net.BindException
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@UsePlaywright
class AdminControllerTest {
    private var port: Int = -1
    private var server: ApplicationEngine? = null
    private var password = AtomicReference<String>()

    @Inject
    private lateinit var memberService: MemberService

    @Inject
    private lateinit var animeService: AnimeService

    @Inject
    private lateinit var episodeService: EpisodeService

    private fun isPortInUse(port: Int): Boolean {
        try {
            val socket = ServerSocket(port)
            socket.close()
            return false
        } catch (e: BindException) {
            return true
        }
    }

    @BeforeEach
    fun setUp() {
        do {
            port = (10000..65535).random()
        } while (isPortInUse(port))

        Constant.injector.injectMembers(this)
        server = initAll(password, port, false)
        JobManager.invalidate()

        val descriptions = listOf(
            "The story of the Saiyan Goku and his friends continues after the defeat of Majin Buu.",
            "C'est l'histoire de la suite de Dragon Ball Z. L'histoire se passe 10 ans après la défaite de Majin Buu.",
            "(Test) - The story of the Saiyan Goku and his friends continues after the defeat of Majin Buu.",
            "(Test) - C'est l'histoire de la suite de Dragon Ball Z. L'histoire se passe 10 ans après la défaite de Majin Buu."
        )

        val listFiles = ClassLoader.getSystemClassLoader().getResource("animes")?.file?.let { File(it).listFiles() }

        listFiles
            ?.sortedBy { it.name.lowercase() }
            ?.forEach {
                val anime = animeService.save(
                    AbstractConverter.convert(
                        ObjectParser.fromJson(
                            it.readText(),
                            AnimeDto::class.java
                        ), Anime::class.java
                    )
                )

                (1..10).forEach { number ->
                    episodeService.save(
                        Episode(
                            platform = Platform.entries.random(),
                            anime = anime,
                            episodeType = EpisodeType.entries.random(),
                            langType = LangType.entries.random(),
                            hash = UUID.randomUUID().toString(),
                            releaseDateTime = anime.releaseDateTime,
                            season = 1,
                            number = number,
                            title = "Episode $number",
                            description = descriptions[number % descriptions.size],
                            url = "https://www.google.com",
                            image = "https://pbs.twimg.com/profile_banners/1726908281640091649/1700562801/1500x500",
                            duration = 1420
                        )
                    )
                }
            }
    }

    @AfterEach
    fun tearDown() {
        memberService.deleteAll()
        server?.stop(1000, 10000)
        episodeService.deleteAll()
        animeService.deleteAll()
    }

    @Test
    fun `test admin login and all links`(page: Page) {
        page.navigate("http://localhost:$port/admin")
        assertEquals("Login - Shikkanime", page.title())
        page.fill("input[name=username]", "admin")
        page.fill("input[name=password]", password.get())
        page.click("button[type=submit]")

        Link.entries.filter { it.href.startsWith("/admin") }.forEach {
            val allA = page.querySelectorAll("a")
                .distinctBy { a -> a.getAttribute("href") }
                .groupBy { a -> a.getAttribute("href") }

            val currentA = allA[it.href]?.firstOrNull()
            assertEquals(true, currentA != null)
            currentA!!.click()
            page.waitForTimeout(1000.0)
            val s = it.label + " - Shikkanime"
            println(s)
            assertEquals(s, page.title())
        }
    }

    @Test
    fun `create netflix simulcast`(page: Page) {
        page.navigate("http://localhost:$port/admin")
        assertEquals("Login - Shikkanime", page.title())
        page.fill("input[name=username]", "admin")
        page.fill("input[name=password]", password.get())
        page.click("button[type=submit]")

        page.navigate("http://localhost:$port${Link.PLATFORMS.href}")
        page.click("button[data-bs-target='#collapse${Platform.NETF.name}']")
        page.click("a[href='${Link.PLATFORMS.href}/${Platform.NETF.name}/simulcasts']")

        page.fill("input[name=name]", "81564899")
        page.fill("input[name=releaseDay]", "4")
        page.fill("input[name=image]", "https://cdn.myanimelist.net/images/anime/1938/140374.jpg")
        page.fill("input[name=releaseTime]", "13:30")
        page.fill("input[name=season]", "2")

        page.click("button[type=submit]")
    }

    @Test
    fun `create prime video simulcast`(page: Page) {
        page.navigate("http://localhost:$port/admin")
        assertEquals("Login - Shikkanime", page.title())
        page.fill("input[name=username]", "admin")
        page.fill("input[name=password]", password.get())
        page.click("button[type=submit]")

        page.navigate("http://localhost:$port${Link.PLATFORMS.href}")
        page.click("button[data-bs-target='#collapse${Platform.PRIM.name}']")
        page.click("a[href='${Link.PLATFORMS.href}/${Platform.PRIM.name}/simulcasts']")

        page.fill("input[name=name]", "0QN9ZXJ935YBTNK8U9FV5OAX5B")
        page.fill("input[name=releaseDay]", "1")
        page.fill("input[name=image]", "https://cdn.myanimelist.net/images/anime/1142/141351.jpg")
        page.fill("input[name=releaseTime]", "17:01")

        page.click("button[type=submit]")
    }

    @Test
    fun `invalidate simulcasts`(page: Page) {
        page.navigate("http://localhost:$port/admin")
        assertEquals("Login - Shikkanime", page.title())
        page.fill("input[name=username]", "admin")
        page.fill("input[name=password]", password.get())
        page.click("button[type=submit]")

        page.click("#simulcasts-invalidate")
    }
}