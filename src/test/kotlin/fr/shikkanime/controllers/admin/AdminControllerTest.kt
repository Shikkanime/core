package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import com.microsoft.playwright.Playwright
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.module
import fr.shikkanime.services.MemberService
import fr.shikkanime.utils.Constant
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AdminControllerTest {
    private val port = (10000..65535).random()
    private var server: ApplicationEngine? = null

    @Inject
    private lateinit var memberService: MemberService

    @BeforeEach
    fun setUp() {
        Constant.injector.injectMembers(this)

        val environment = applicationEngineEnvironment {
            module {
                module()
            }

            connector {
                host = "localhost"
                port = this@AdminControllerTest.port
            }
        }

        server = embeddedServer(Netty, environment).start(false)
    }

    @AfterEach
    fun tearDown() {
        memberService.deleteAll()
        server?.stop(1000, 10000)
    }

    @Test
    fun `test admin login`() {
        val password = memberService.initDefaultAdminUser()

        val playwright = Playwright.create()
        val browser = playwright.chromium().launch()
        val page = browser.newPage()
        page.navigate("http://localhost:$port/admin")
        assertEquals("Login - Shikkanime", page.title())
        page.fill("input[name=username]", "admin")
        page.fill("input[name=password]", password)
        page.click("button[type=submit]")

        Link.entries.filter { it.href.startsWith("/admin") }.forEach {
            val allA = page.querySelectorAll("a")
                .distinctBy { a -> a.getAttribute("href") }
                .groupBy { a -> a.getAttribute("href") }

            val currentA = allA[it.href]?.firstOrNull()
            assertEquals(true, currentA != null)
            currentA!!.click()
            page.waitForTimeout(5000.0)
            val s = it.label + " - Shikkanime"
            println(s)
            assertEquals(s, page.title())
        }

        page.close()
        browser.close()
        playwright.close()
    }
}