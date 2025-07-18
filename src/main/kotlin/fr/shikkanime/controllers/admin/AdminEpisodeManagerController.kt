package fr.shikkanime.controllers.admin

import com.google.inject.Inject
import fr.shikkanime.entities.enums.CountryCode
import fr.shikkanime.entities.enums.EpisodeType
import fr.shikkanime.entities.enums.Link
import fr.shikkanime.entities.enums.Platform
import fr.shikkanime.platforms.AbstractPlatform
import fr.shikkanime.services.EpisodeVariantService
import fr.shikkanime.utils.Constant
import fr.shikkanime.utils.FileManager
import fr.shikkanime.utils.HttpRequest
import fr.shikkanime.utils.MapCache
import fr.shikkanime.utils.routes.AdminSessionAuthenticated
import fr.shikkanime.utils.routes.Controller
import fr.shikkanime.utils.routes.Path
import fr.shikkanime.utils.routes.Response
import fr.shikkanime.utils.routes.method.Get
import fr.shikkanime.utils.routes.method.Post
import fr.shikkanime.utils.routes.param.BodyParam
import fr.shikkanime.utils.routes.param.QueryParam
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.io.readByteArray
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZonedDateTime

@Controller("/admin/episode-manager")
class AdminEpisodeManagerController {
    private val httpRequest = HttpRequest()

    @Inject private lateinit var episodeVariantService: EpisodeVariantService

    @Path
    @Get
    @AdminSessionAuthenticated
    private fun getEpisodeManager(): Response {
        val files = File(Constant.exportsFolder, "/").listFiles()
            ?.filter { it.name.endsWith(".xlsx") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        return Response.template(
            Link.EPISODE_MANAGER,
            mutableMapOf(
                "files" to files
            )
        )
    }

    @Path("/download")
    @Get
    @AdminSessionAuthenticated
    private fun getDownload(@QueryParam file: String): Response {
        val requestedFile = File(Constant.exportsFolder, file)

        if (!requestedFile.exists() || !requestedFile.name.endsWith(".xlsx")) {
            return Response.redirect("${Link.EPISODE_MANAGER.href}?error=${URLEncoder.encode("File not found", StandardCharsets.UTF_8)}")
        }

        return Response.multipart(requestedFile.readBytes(), ContentType.Application.OctetStream)
    }

    @Path("/import")
    @Post
    @AdminSessionAuthenticated
    private fun postImport(@BodyParam multiPartData: MultiPartData): Response {
        var bytes: ByteArray? = null

        runBlocking {
            multiPartData.forEachPart { part ->
                if (part is PartData.FileItem) {
                    bytes = part.provider().readRemaining().readByteArray()
                }

                part.dispose()
            }
        }

        requireNotNull(bytes) { "No file provided" }
        val file = File(Constant.exportsFolder, "imported_${System.currentTimeMillis()}.xlsx")
        file.outputStream().buffered().use { it.write(bytes) }

        // Reuse logic from ImportFile.kt
        // This is a simplified version, you might need to adjust it based on your exact needs
        val workbook = XSSFWorkbook(file.inputStream())

        val episodes = workbook.sheetIterator().asSequence().flatMap { sheet ->
            sheet.rowIterator().asSequence().mapIndexedNotNull { index, row ->
                if (index == 0) return@mapIndexedNotNull null

                try {
                    AbstractPlatform.Episode(
                        CountryCode.valueOf(row.getCell(0)?.stringCellValue?.uppercase() ?: return@mapIndexedNotNull null),
                        row.getCell(1)?.stringCellValue ?: return@mapIndexedNotNull null,
                        row.getCell(2)?.stringCellValue ?: return@mapIndexedNotNull null,
                        row.getCell(3)?.stringCellValue ?: return@mapIndexedNotNull null,
                        row.getCell(4)?.stringCellValue ?: return@mapIndexedNotNull null,
                        row.getCell(5)?.stringCellValue,
                        ZonedDateTime.ofInstant(row.getCell(6)?.dateCellValue?.toInstant() ?: return@mapIndexedNotNull null, Constant.utcZoneId),
                        EpisodeType.valueOf(row.getCell(7)?.stringCellValue ?: return@mapIndexedNotNull null),
                        row.getCell(8)?.stringCellValue ?: return@mapIndexedNotNull null,
                        row.getCell(9)?.numericCellValue?.toInt() ?: return@mapIndexedNotNull null,
                        row.getCell(10)?.numericCellValue?.toInt() ?: return@mapIndexedNotNull null,
                        row.getCell(11)?.numericCellValue?.toLong() ?: return@mapIndexedNotNull null,
                        row.getCell(12)?.stringCellValue,
                        row.getCell(13)?.stringCellValue,
                        row.getCell(14)?.stringCellValue ?: return@mapIndexedNotNull null,
                        Platform.valueOf(row.getCell(15)?.stringCellValue ?: return@mapIndexedNotNull null),
                        row.getCell(16)?.stringCellValue ?: return@mapIndexedNotNull null,
                        row.getCell(17)?.stringCellValue ?: return@mapIndexedNotNull null,
                        row.getCell(18)?.stringCellValue ?: return@mapIndexedNotNull null,
                        row.getCell(19)?.stringCellValue == "true",
                        row.getCell(20)?.stringCellValue == "true"
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }.toList()

        episodes.forEach { episodeVariantService.save(it) }
        workbook.close()

        return Response.redirect("${Link.EPISODE_MANAGER.href}?success=${URLEncoder.encode("File imported successfully", StandardCharsets.UTF_8)}")
    }

    private suspend fun getAnimeTitles(): Document {
        val response = httpRequest.get("https://anidb.net/api/anime-titles.xml.gz")
        require(response.status == HttpStatusCode.OK) { "Failed to get anime titles" }
        return Jsoup.parse(String(FileManager.decompressGzip(response.bodyAsBytes())))
    }

    private fun getAniDBAnimeTitles(locale: String) = MapCache.getOrCompute(
        "EpisodeManagerController.getAniDBAnimeTitles",
        key = locale,
    ) { locale ->
        runBlocking { getAnimeTitles().select("anime")
            .asSequence()
            .associate { it.attr("aid").toLong() to it.select("title[xml:lang=\"${locale.split("-").first()}\"],title[xml:lang=\"x-jat\"]").asSequence().map { it.text() }.toSet() }
            .filter { it.value.isNotEmpty() }
        } }

    private suspend fun getAniDBAnime(id: Long): Document {
        val response = httpRequest.get("http://api.anidb.net:9001/httpapi?request=anime&client=xxx&clientver=1&protover=1&aid=$id")
        require(response.status == HttpStatusCode.OK) { "Failed to get anime" }
        return Jsoup.parse(response.bodyAsText())
    }

    data class AniDBEpisode(
        val id: Int,
        val resources: Map<Int, Set<String>>,
        val number: String,
        val airdate: LocalDate
    )

    private fun getAniDBEpisodesByAnime(animeId: Long) = MapCache.getOrCompute(
        "EpisodeManagerController.getAniDBEpisodesByAnime",
        key = animeId,
    ) { animeId -> runBlocking {
        getAniDBAnime(animeId).select("episodes > episode").mapNotNull {
            val airDateString = it.select("airdate").text().takeIf { it.isNotBlank() } ?: return@mapNotNull null

            AniDBEpisode(
                id = it.attr("id").toInt(),
                resources = it.select("resources > resource").asSequence().associate { it.attr("type").toInt() to it.select("identifier").map { it.text() }.toSet() },
                number = it.select("epno").text(),
                airdate = LocalDate.parse(airDateString)
            )
        }.sortedBy { it.airdate }
    } }

    @Path("/anidb-update")
    @Post
    @AdminSessionAuthenticated
    private suspend fun postAnidbUpdate(parts: List<PartData>): Response {
        var animeName: String? = null
        var filePart: PartData.FileItem? = null

        parts.forEach { part ->
            when (part) {
                is PartData.FormItem -> {
                    if (part.name == "animeName") {
                        animeName = part.value
                    }
                }
                is PartData.FileItem -> {
                    if (part.name == "anidbFile") {
                        filePart = part
                    }
                }
                else -> {
                    // Do nothing
                }
            }

            part.dispose()
        }

        if (animeName.isNullOrBlank() || filePart == null) {
            return Response.redirect("${Link.EPISODE_MANAGER.href}?error=${URLEncoder.encode("Anime name and file are required.", StandardCharsets.UTF_8)}")
        }

        val uploadedFile = File(Constant.exportsFolder, "tmp_${filePart.originalFileName}")
        uploadedFile.outputStream().buffered().use { it.write(filePart.provider().readRemaining().readByteArray()) }
        filePart.dispose()

        val workbook = XSSFWorkbook(uploadedFile.inputStream())
        val rows = workbook.sheetIterator().asSequence().flatMap { sheet ->
            sheet.rowIterator().asSequence().filter { row ->
                val cell = row.getCell(2)
                cell != null && cell.stringCellValue.equals(animeName, true)
            }
        }.toList()

        if (rows.isEmpty()) {
            uploadedFile.delete()
            return Response.redirect("${Link.EPISODE_MANAGER.href}?error=${URLEncoder.encode("No rows found for anime '$animeName'", StandardCharsets.UTF_8)}")
        }

        val animeTitles = getAniDBAnimeTitles("fr-FR")
        val animeId = animeTitles.filter { it.value.any { title -> title.equals(animeName, true) } }.keys.firstOrNull()

        if (animeId == null) {
            uploadedFile.delete()
            return Response.redirect("${Link.EPISODE_MANAGER.href}?error=${URLEncoder.encode("Anime '$animeName' not found on AniDB.", StandardCharsets.UTF_8)}")
        }

        val aniDBEpisodes = getAniDBEpisodesByAnime(animeId)

        rows.forEach { row ->
            val number = row.getCell(10).numericCellValue.toInt()
            val airdateCell = row.getCell(6)
            val episode = aniDBEpisodes.firstOrNull { it.number == number.toString() }

            if (episode != null) {
                airdateCell?.setCellValue(episode.airdate.atStartOfDay())
                val cellStyle = workbook.createCellStyle()
                cellStyle.dataFormat = workbook.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss")
                airdateCell?.cellStyle = cellStyle
            }
        }

        val outputFileName = "updated_${animeName.replace(" ", "_")}_${System.currentTimeMillis()}.xlsx"
        val outputFile = File(Constant.exportsFolder, outputFileName)
        val outputStream = outputFile.outputStream()
        workbook.write(outputStream)
        outputStream.close()
        workbook.close()
        uploadedFile.delete()

        return Response.multipart(outputFile.readBytes(), ContentType.Application.OctetStream)
    }
}
