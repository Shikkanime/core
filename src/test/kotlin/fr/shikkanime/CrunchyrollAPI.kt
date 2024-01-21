package fr.shikkanime

import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.platforms.CrunchyrollPlatform
import fr.shikkanime.utils.Constant
import java.time.ZonedDateTime
import kotlin.system.exitProcess

fun main() {
    val now = ZonedDateTime.now()
    val crunchyrollPlatform = Constant.injector.getInstance(CrunchyrollPlatform::class.java)
    crunchyrollPlatform.configuration?.availableCountries?.add(CountryCode.FR)

    for (i in 1..5) {
        println("Fetching episodes from Crunchyroll $i/5")
        crunchyrollPlatform.fetchEpisodes(now)
        println("Sleeping for 5 seconds")
        Thread.sleep(5000)
        crunchyrollPlatform.simulcasts.invalidate()
        crunchyrollPlatform.animeInfoCache.invalidate()
        crunchyrollPlatform.hashCache.clear()
    }

    exitProcess(0)
}