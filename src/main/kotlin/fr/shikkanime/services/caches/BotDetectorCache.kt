package fr.shikkanime.services.caches

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.StringReader
import java.net.InetAddress
import java.time.Duration
import kotlin.experimental.and

class BotDetectorCache : AbstractCacheService {
    companion object {
        private const val DEFAULT_ALL_KEY = "all"
    }

    private val ipv4Regex = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")

    @Inject
    private lateinit var configCacheService: ConfigCacheService

    private val ipGlobalCache = MapCache<String, List<String>>(
        "BotDetectorCache.ipGlobalCache",
        duration = Duration.ofDays(1),
        fn = { listOf(DEFAULT_ALL_KEY) }
    ) {
        runBlocking {
            val response = HttpRequest().get("https://raw.githubusercontent.com/AnTheMaker/GoodBots/refs/heads/main/all.ips")

            if (response.status != HttpStatusCode.OK) {
                return@runBlocking emptyList()
            }

            response.bodyAsText().split("\n").filter { it.isNotBlank() || !ipv4Regex.matches(it) }
        }
    }

    private val regexGlobalCache = MapCache<String, List<Regex>>(
        "BotDetectorCache.regexGlobalCache",
        duration = Duration.ofDays(1),
        fn = { listOf(DEFAULT_ALL_KEY) }
    ) {
        runBlocking {
            val response = HttpRequest().get("https://raw.githubusercontent.com/matomo-org/device-detector/refs/heads/master/regexes/bots.yml")

            if (response.status != HttpStatusCode.OK) {
                return@runBlocking emptyList()
            }

            val bodyAsText = response.bodyAsText()
            val mapper = ObjectMapper(YAMLFactory())
            val yaml = mapper.readTree(StringReader(bodyAsText))

            yaml.map { it["regex"].asText() }.map { it.toRegex() }
        }
    }

    private fun createMask(cidr: Int): ByteArray {
        val mask = ByteArray(4)
        for (i in 0 until 4) {
            mask[i] = ((0xFF shl (8 - minOf(cidr - 8 * i, 8))) and 0xFF).toByte()
        }
        return mask
    }


    private fun isInRange(ip: ByteArray, network: ByteArray, mask: ByteArray): Boolean {
        for (i in 0 until 4) {
            if ((ip[i] and mask[i]) != (network[i] and mask[i])) {
                return false
            }
        }

        return true
    }

    fun isBot(clientIp: String? = null, userAgent: String? = null): Boolean {
        if (!clientIp.isNullOrBlank()) {
            val clientIpBytes = InetAddress.getByName(clientIp).address

            ipGlobalCache[DEFAULT_ALL_KEY]?.forEach { botIp ->
                if (botIp.contains("/")) {
                    val (ip, cidr) = botIp.split("/")
                    val mask = createMask(cidr.toInt())
                    val networkIpBytes = InetAddress.getByName(ip).address

                    if (isInRange(clientIpBytes, networkIpBytes, mask)) {
                        return true
                    }
                } else if (clientIp == botIp) {
                    return true
                }
            }
        }

        if (!userAgent.isNullOrBlank()) {
            val regexes = regexGlobalCache[DEFAULT_ALL_KEY]?.toMutableSet() ?: mutableSetOf()
            configCacheService.getValueAsString(ConfigPropertyKey.BOT_ADDITIONAL_REGEX)?.toRegex()?.let { regexes.add(it) }

            regexes.forEach { regex ->
                if (userAgent.contains(regex)) {
                    return true
                }
            }
        }

        return false
    }
}