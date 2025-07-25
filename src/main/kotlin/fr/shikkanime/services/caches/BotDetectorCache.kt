package fr.shikkanime.services.caches

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.inject.Inject
import fr.shikkanime.entities.enums.ConfigPropertyKey
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.StringUtils
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.StringReader
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.experimental.and

class BotDetectorCache : ICacheService {
    private val httpRequest = HttpRequest()
    private val ipv4Regex = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?:/[0-9]+)*")

    @Inject private lateinit var configCacheService: ConfigCacheService

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

    private fun getGoodBotsIPs() = MapCache.getOrCompute(
        "BotDetectorCache.getGoodBotsIPs",
        key = StringUtils.EMPTY_STRING
    ) {
        runBlocking {
            val response =
                httpRequest.get("https://raw.githubusercontent.com/AnTheMaker/GoodBots/refs/heads/main/all.ips")

            if (response.status != HttpStatusCode.OK) {
                return@runBlocking CopyOnWriteArraySet()
            }

            CopyOnWriteArraySet(response.bodyAsText().split("\n").filter { it.isNotBlank() && ipv4Regex.matches(it) })
        }
    }

    private fun getGoodBotsRegex() = MapCache.getOrCompute(
        "BotDetectorCache.getGoodBotsRegex",
        key = StringUtils.EMPTY_STRING
    ) {
        runBlocking {
            val response = httpRequest.get("https://raw.githubusercontent.com/matomo-org/device-detector/refs/heads/master/regexes/bots.yml")

            if (response.status != HttpStatusCode.OK) {
                return@runBlocking CopyOnWriteArraySet()
            }

            val bodyAsText = response.bodyAsText()
            val mapper = ObjectMapper(YAMLFactory())
            val yaml = mapper.readTree(StringReader(bodyAsText))

            CopyOnWriteArraySet(yaml.map { it["regex"].asText().toRegex() })
        }
    }

    private fun getAbuseIPs() = MapCache.getOrCompute(
        "BotDetectorCache.getAbuseIPs",
        key = StringUtils.EMPTY_STRING
    ) {
        runBlocking {
            val response =
                httpRequest.get("https://raw.githubusercontent.com/borestad/blocklist-abuseipdb/main/abuseipdb-s100-30d.ipv4")

            if (response.status != HttpStatusCode.OK) {
                return@runBlocking CopyOnWriteArraySet()
            }

            CopyOnWriteArraySet(response.bodyAsText().split("\n").filter { it.isNotBlank() && ipv4Regex.matches(it) })
        }
    }

    private fun isIpInRange(clientIp: String, botIp: String): Boolean {
        return if ("/" in botIp) {
            val (ip, cidr) = botIp.split("/")
            val mask = createMask(cidr.toInt())
            isInRange(InetAddress.getByName(clientIp).address, InetAddress.getByName(ip).address, mask)
        } else clientIp == botIp
    }

    fun isBot(clientIp: String? = null, userAgent: String? = null): Boolean {
        clientIp?.let { ip ->
            if (ip in getAbuseIPs() || getGoodBotsIPs().any { isIpInRange(ip, it) }) return true
        }

        userAgent?.let { agent ->
            val regexes = getGoodBotsRegex()

            configCacheService.getValueAsString(ConfigPropertyKey.BOT_ADDITIONAL_REGEX)
                ?.toRegex()
                ?.let { regexes.add(it) }

            if (regexes.any { it in agent }) return true
        }

        return false
    }
}