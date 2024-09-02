package fr.shikkanime

import fr.shikkanime.entities.CalendarEntry
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.LangType
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.services.ImageService.setRenderingHints
import fr.shikkanime.utils.*
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.system.exitProcess

private val flagImage = ImageIO.read(URI("https://flagpedia.net/data/flags/emoji/twitter/256x256/fr.png").toURL()).resize(25, 25)

fun main() {
    val episodeVariantService = Constant.injector.getInstance(EpisodeVariantService::class.java)
    val countryCode = CountryCode.FR

    val now = ZonedDateTime.parse("2022-10-22T00:00:00Z[UTC]")
//    val now = ZonedDateTime.parse("2024-06-19T00:00:00Z[UTC]")
//    val now = ZonedDateTime.now()
    val zoneId = ZoneId.of(countryCode.timezone)
    val atStartOfTheDay = now.toLocalDate().atStartOfDay(zoneId)
    val atEndOfTheDay = now.plusDays(1).toLocalDate().atStartOfDay(zoneId).minusSeconds(1)

    val allEntries = episodeVariantService.findAllAnimeEpisodeMappingReleaseDateTimePlatformAudioLocaleByDateRange(
        countryCode,
        null,
        atStartOfTheDay,
        atEndOfTheDay
    )

    val bufferedImage = BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB)
    val graphics = bufferedImage.createGraphics().apply { setRenderingHints() }
    var backgroundImage: BufferedImage

    do {
        backgroundImage = ImageIO.read(URI(allEntries.random().episodeMapping.image!!).toURL())
    } while (backgroundImage.width < bufferedImage.width || backgroundImage.height < bufferedImage.height)

    val ratioHeight = bufferedImage.height.toDouble() / backgroundImage.height
    val ratioBackgroundImage = backgroundImage.ratioResize(ratioHeight)
    graphics.drawImage(ratioBackgroundImage, (bufferedImage.width - ratioBackgroundImage.width) / 2, (bufferedImage.height - ratioBackgroundImage.height) / 2, null)
    graphics.color = Color(0, 0, 0, 141)
    graphics.fillRect(0, 0, bufferedImage.width, bufferedImage.height)
    graphics.color = Color.WHITE
    graphics.font = graphics.font.deriveFont(30f)
    graphics.drawString("Sorties du ${now.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))}", 10, 40)

    var y = 100

    allEntries
        .sortedByDescending { it.releaseDateTime }
        .distinctBy { "${it.anime.uuid}|${it.platform}|${it.episodeMapping.season}|${it.episodeMapping.episodeType}|${it.audioLocale}" }
        .sortedWith(compareBy({ it.releaseDateTime }, { it.anime.name }, { LangType.fromAudioLocale(it.anime.countryCode!!, it.audioLocale) }))
        .groupBy { it.platform }
        .toSortedMap()
        .forEach { (platform, entries) ->
            val platformImage = ImageIO.read(FileManager.getInputStreamFromResource("assets/img/platforms/banner/${platform.banner}"))
            val platformImageResized = platformImage.ratioResize(40.0 / platformImage.height)

            graphics.drawImage(platformImageResized, 10, y - (platformImageResized.height / 2), null)
            y += platformImageResized.height + 10

            entries.forEach {
                y = drawReleaseLine(bufferedImage.width, graphics, 40, y, it, allEntries)
            }

            y += 30
        }

    ImageIO.write(bufferedImage, "jpg", File("calendar.jpg"))
    exitProcess(0)
}

fun drawReleaseLine(
    width: Int, graphics: Graphics2D, marginHorizontal: Int, y: Int,
    calendarEntry: CalendarEntry, allEntries: List<CalendarEntry>
): Int {
    // Variable declarations
    val infoX = 725
    val maxAnimeWidth = infoX - marginHorizontal - 10
    val flagMargin = 10
    val label = StringUtils.getShortName(calendarEntry.anime.name!!) +
            (calendarEntry.episodeMapping.season!!.takeIf { it > 1 }?.let { " S$it" } ?: "")
    val isSubbed = LangType.fromAudioLocale(calendarEntry.anime.countryCode!!, calendarEntry.audioLocale) == LangType.VOICE
    val animeWidth = graphics.fontMetrics.stringWidth(label) + if (isSubbed) flagImage.width + flagMargin else 0
    val onTwoLines = animeWidth > maxAnimeWidth
    val (firstLine, secondLine) = if (onTwoLines) {
        label.split(" ").let {
            it.take((it.size * 2 / 3.0).toInt()).joinToString(" ") to
                    it.drop((it.size * 2 / 3.0).toInt()).joinToString(" ")
        }
    } else {
        label to ""
    }
    val relevantEntries = allEntries.filter {
        it.anime.uuid == calendarEntry.anime.uuid &&
                it.episodeMapping.season == calendarEntry.episodeMapping.season &&
                it.audioLocale == calendarEntry.audioLocale
    }
    val minEpisode = relevantEntries.minByOrNull { it.episodeMapping.number!! }
    val maxEpisode = relevantEntries.maxByOrNull { it.episodeMapping.number!! }
    val isAllEpisodes = abs(maxEpisode!!.episodeMapping.number!! - minEpisode!!.episodeMapping.number!!) > 5
    val episodeLabel = when {
        isAllEpisodes -> "Intégralité"
        maxEpisode.episodeMapping.number!! == minEpisode.episodeMapping.number!! -> "Épisode ${minEpisode.episodeMapping.number!!}"
        else -> "Épisodes ${minEpisode.episodeMapping.number!!}-${maxEpisode.episodeMapping.number!!}"
    }
    val releaseTime = calendarEntry.releaseDateTime.withZoneSameInstant(
        ZoneId.of(calendarEntry.anime.countryCode.timezone)
    ).format(DateTimeFormatter.ofPattern("HH:mm"))

    // Drawing logic
    graphics.color = Color.WHITE
    graphics.font = graphics.font.deriveFont(20f).deriveFont(graphics.font.style and Font.BOLD.inv())

    if (isSubbed) {
        graphics.drawImage(flagImage, marginHorizontal, y - 20, null)
    }

    val textX = marginHorizontal + if (isSubbed) flagImage.width + flagMargin else 0
    if (onTwoLines) {
        graphics.drawString(firstLine, textX, y)
        graphics.drawString(secondLine, textX, y + 30)
    } else {
        graphics.drawString(label, textX, y)
    }

    graphics.drawString(episodeLabel, infoX, if (onTwoLines) y + 15 else y)
    graphics.drawString(releaseTime, width - marginHorizontal - graphics.fontMetrics.stringWidth(releaseTime), if (onTwoLines) y + 15 else y)

    graphics.color = Color(255, 255, 255, 100)
    graphics.drawLine(marginHorizontal, y + if (onTwoLines) 35 else 5, width - marginHorizontal, y + if (onTwoLines) 35 else 5)

    return y + if (onTwoLines) 60 else 30
}