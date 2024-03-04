package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import com.microsoft.playwright.Playwright
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.initAll
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.JobManager
import io.ktor.server.engine.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.BindException
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference

class AdminControllerTest {
    private var port: Int = -1
    private var server: ApplicationEngine? = null
    private var password = AtomicReference<String>()

    @Inject
    private lateinit var memberService: MemberService

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
    }

    @AfterEach
    fun tearDown() {
        memberService.deleteAll()
        server?.stop(1000, 10000)
    }

    @Test
    fun `test admin login and all links`() {
        val playwright = Playwright.create()
        val browser = playwright.chromium().launch()
        val page = browser.newPage()
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

        page.close()
        browser.close()
        playwright.close()
    }

    @Test
    fun `create netflix simulcast`() {
        val playwright = Playwright.create()
        val browser = playwright.chromium().launch()
        val page = browser.newPage()

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

        page.close()
        browser.close()
        playwright.close()
    }

    @Test
    fun `create prime video simulcast`() {
        val playwright = Playwright.create()
        val browser = playwright.chromium().launch()
        val page = browser.newPage()

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

        page.close()
        browser.close()
        playwright.close()
    }
}